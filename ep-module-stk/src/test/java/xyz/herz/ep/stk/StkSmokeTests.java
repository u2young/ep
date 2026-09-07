package xyz.herz.ep.stk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.stk.entity.batch.StkBatch;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;
import xyz.herz.ep.stk.entity.entry.StkStockEntryItem;
import xyz.herz.ep.stk.entity.reconciliation.StkReconciliationItem;
import xyz.herz.ep.stk.entity.reconciliation.StkStockReconciliation;
import xyz.herz.ep.stk.entity.serial.StkSerialNo;
import xyz.herz.ep.stk.entity.settings.StkStockSettings;
import xyz.herz.ep.stk.enums.StkDictEnums.EnableStatus;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryStatus;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryType;
import xyz.herz.ep.stk.enums.StkDictEnums.ReconciliationStatus;
import xyz.herz.ep.stk.handler.entry.StkEntryLifecycleHandler;
import xyz.herz.ep.stk.handler.reconciliation.StkReconciliationLifecycleHandler;
import xyz.herz.ep.stk.jpa.batch.StkBatchRepository;
import xyz.herz.ep.stk.jpa.entry.StkStockEntryRepository;
import xyz.herz.ep.stk.jpa.reconciliation.StkStockReconciliationRepository;
import xyz.herz.ep.stk.jpa.serial.StkSerialNoRepository;
import xyz.herz.ep.stk.jpa.settings.StkStockSettingsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存管理模块冒烟测试(参考 ERPNext Stock DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-J 验收矩阵:
 * <ol>
 *   <li>Stk1 出入库单生命周期:提交(回写 totalQty/totalAmount/submittedAt) + 取消</li>
 *   <li>Stk2 出入库类型守卫:入库缺目标仓/出库缺来源仓/移库缺双仓被拒</li>
 *   <li>Stk3 盘点单生命周期:提交(回写 variance+合计) + 差异计算(盘盈/盘亏)</li>
 *   <li>Stk4 序列号 + 批次主数据 CRUD + 按物料/状态查询</li>
 *   <li>Stk5 状态机守卫:DataProxy status 禁止表单直改 + 已取消不可再取消</li>
 *   <li>Stk6 库存配置单例:Initializer 幂等 + 默认值 + 修改回读</li>
 * </ol>
 */
@SpringBootTest(classes = StkTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class StkSmokeTests {

    @Autowired StkStockEntryRepository entryRepo;
    @Autowired StkStockReconciliationRepository reconRepo;
    @Autowired StkSerialNoRepository serialRepo;
    @Autowired StkBatchRepository batchRepo;
    @Autowired StkStockSettingsRepository settingsRepo;
    @Autowired StkEntryLifecycleHandler entryLifecycle;
    @Autowired StkReconciliationLifecycleHandler reconLifecycle;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private StkStockEntry buildEntry(String no, Integer type, String srcWh, String tgtWh) {
        StkStockEntry e = new StkStockEntry();
        e.setEntryNo(no);
        e.setEntryType(type);
        e.setSourceWarehouseCode(srcWh);
        e.setSourceWarehouseName(srcWh == null ? null : "来源仓-" + srcWh);
        e.setTargetWarehouseCode(tgtWh);
        e.setTargetWarehouseName(tgtWh == null ? null : "目标仓-" + tgtWh);
        e.setPostingDate(LocalDate.now());
        e.setStatus(EntryStatus.DRAFT.code);

        StkStockEntryItem i1 = new StkStockEntryItem();
        i1.setEntry(e);
        i1.setItemCode("ITEM-A");
        i1.setItemName("测试物料A");
        i1.setQty(bd("10"));
        i1.setUnitPrice(bd("100.00"));
        // amount 留空,验证提交时自动计算

        StkStockEntryItem i2 = new StkStockEntryItem();
        i2.setEntry(e);
        i2.setItemCode("ITEM-B");
        i2.setItemName("测试物料B");
        i2.setQty(bd("5"));
        i2.setUnitPrice(bd("200.00"));

        e.setItems(new ArrayList<>(List.of(i1, i2)));
        return entryRepo.save(e);
    }

    // =================== (1) 出入库单生命周期 ===================
    @Test
    void stk1_entry_lifecycle() {
        StkStockEntry e = buildEntry("SE-T-001", EntryType.MATERIAL_RECEIPT.code,
            null, "WH-TGT");

        // 提交:回写 totalQty/totalAmount + 明细 amount 自动计算
        String subR = entryLifecycle.exec(List.of(e), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(subR.contains("成功 1"), "提交应成功,实际=" + subR);
        StkStockEntry submitted = entryRepo.findById(e.getId()).orElseThrow();
        assertEquals(EntryStatus.SUBMITTED.code, submitted.getStatus(), "状态=已提交");
        // totalQty = 10 + 5 = 15;totalAmount = 1000 + 1000 = 2000
        assertEquals(0, submitted.getTotalQty().compareTo(bd("15")),
            "totalQty=15,实际=" + submitted.getTotalQty());
        assertEquals(0, submitted.getTotalAmount().compareTo(bd("2000.00")),
            "totalAmount=2000,实际=" + submitted.getTotalAmount());
        assertEquals(0, submitted.getItems().get(0).getAmount().compareTo(bd("1000.00")),
            "明细1小计=1000,实际=" + submitted.getItems().get(0).getAmount());
        assertNotNull(submitted.getSubmittedAt(), "submittedAt 应回写");
        assertEquals("system", submitted.getOperator(), "operator 默认=system");

        // 取消:已提交 → 已取消
        String cancelR = entryLifecycle.exec(List.of(submitted), null,
            new String[]{StkEntryLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("成功 1"), "取消应成功,实际=" + cancelR);
        assertEquals(EntryStatus.CANCELLED.code,
            entryRepo.findById(e.getId()).orElseThrow().getStatus(), "状态=已取消");

        // 按类型/状态查询
        assertEquals(1, entryRepo.findByEntryType(EntryType.MATERIAL_RECEIPT.code).size(),
            "入库类型应 1 条");
        assertEquals(1, entryRepo.findByStatus(EntryStatus.CANCELLED.code).size(),
            "已取消状态应 1 条");
        assertEquals(1, entryRepo.findByTargetWarehouseCode("WH-TGT").size(),
            "WH-TGT 目标仓应 1 条");
    }

    // =================== (2) 出入库类型守卫 ===================
    @Test
    void stk2_entry_type_warehouse_guard() {
        // (a) 入库缺目标仓被拒
        StkStockEntry noTgt = buildEntry("SE-G-001", EntryType.MATERIAL_RECEIPT.code,
            null, null);
        String noTgtR = entryLifecycle.exec(List.of(noTgt), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(noTgtR.contains("失败 1"), "入库缺目标仓应被拒,实际=" + noTgtR);

        // (b) 出库缺来源仓被拒
        StkStockEntry noSrc = buildEntry("SE-G-002", EntryType.MATERIAL_ISSUE.code,
            null, null);
        String noSrcR = entryLifecycle.exec(List.of(noSrc), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(noSrcR.contains("失败 1"), "出库缺来源仓应被拒,实际=" + noSrcR);

        // (c) 移库缺双仓被拒(只有目标仓)
        StkStockEntry noSrc2 = buildEntry("SE-G-003", EntryType.MATERIAL_TRANSFER.code,
            null, "WH-TGT");
        String transferR = entryLifecycle.exec(List.of(noSrc2), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(transferR.contains("失败 1"), "移库缺来源仓应被拒,实际=" + transferR);

        // (d) 移库双仓齐全应成功
        StkStockEntry okTransfer = buildEntry("SE-G-004", EntryType.MATERIAL_TRANSFER.code,
            "WH-SRC", "WH-TGT");
        String okR = entryLifecycle.exec(List.of(okTransfer), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(okR.contains("成功 1"), "移库双仓齐全应成功,实际=" + okR);
        assertEquals(EntryStatus.SUBMITTED.code,
            entryRepo.findById(okTransfer.getId()).orElseThrow().getStatus(), "状态=已提交");

        // (e) 无明细被拒
        StkStockEntry noItems = buildEntry("SE-G-005", EntryType.MATERIAL_RECEIPT.code,
            null, "WH-TGT");
        noItems.setItems(new ArrayList<>());
        String noItemsR = entryLifecycle.exec(List.of(noItems), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(noItemsR.contains("失败 1"), "无明细提交应被拒,实际=" + noItemsR);
    }

    // =================== (3) 盘点单生命周期 + 差异计算 ===================
    @Test
    void stk3_reconciliation_lifecycle() {
        StkStockReconciliation r = new StkStockReconciliation();
        r.setReconciliationNo("SR-T-001");
        r.setWarehouseCode("WH-TGT");
        r.setWarehouseName("目标仓");
        r.setReconcileDate(LocalDate.now());
        r.setStatus(ReconciliationStatus.DRAFT.code);

        StkReconciliationItem i1 = new StkReconciliationItem();
        i1.setReconciliation(r);
        i1.setItemCode("ITEM-A");
        i1.setItemName("测试物料A");
        i1.setBookQty(bd("100"));     // 账面 100
        i1.setActualQty(bd("105"));   // 实际 105 → 盘盈 +5

        StkReconciliationItem i2 = new StkReconciliationItem();
        i2.setReconciliation(r);
        i2.setItemCode("ITEM-B");
        i2.setItemName("测试物料B");
        i2.setBookQty(bd("50"));      // 账面 50
        i2.setActualQty(bd("45"));    // 实际 45 → 盘亏 -5

        r.setItems(new ArrayList<>(List.of(i1, i2)));
        reconRepo.save(r);

        // 提交:回写明细 variance + 主单合计
        String subR = reconLifecycle.exec(List.of(r), null,
            new String[]{StkReconciliationLifecycleHandler.CODE_SUBMIT});
        assertTrue(subR.contains("成功 1"), "提交应成功,实际=" + subR);
        StkStockReconciliation submitted = reconRepo.findById(r.getId()).orElseThrow();
        assertEquals(ReconciliationStatus.SUBMITTED.code, submitted.getStatus(), "状态=已提交");
        // variance: i1 = 105-100 = +5; i2 = 45-50 = -5;totalVariance = 0
        assertEquals(0, submitted.getItems().get(0).getVariance().compareTo(bd("5")),
            "明细1差异=+5(盘盈),实际=" + submitted.getItems().get(0).getVariance());
        assertEquals(0, submitted.getItems().get(1).getVariance().compareTo(bd("-5")),
            "明细2差异=-5(盘亏),实际=" + submitted.getItems().get(1).getVariance());
        // totalBook = 100+50 = 150; totalActual = 105+45 = 150; totalVariance = 0
        assertEquals(0, submitted.getTotalBookQty().compareTo(bd("150")),
            "totalBookQty=150,实际=" + submitted.getTotalBookQty());
        assertEquals(0, submitted.getTotalActualQty().compareTo(bd("150")),
            "totalActualQty=150,实际=" + submitted.getTotalActualQty());
        assertEquals(0, submitted.getTotalVariance().compareTo(bd("0")),
            "totalVariance=0,实际=" + submitted.getTotalVariance());
        assertNotNull(submitted.getSubmittedAt(), "submittedAt 应回写");
        assertEquals("system", submitted.getReconciler(), "reconciler 默认=system");

        // 取消
        String cancelR = reconLifecycle.exec(List.of(submitted), null,
            new String[]{StkReconciliationLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("成功 1"), "取消应成功,实际=" + cancelR);
        assertEquals(ReconciliationStatus.CANCELLED.code,
            reconRepo.findById(r.getId()).orElseThrow().getStatus(), "状态=已取消");

        // 按仓库查询
        assertEquals(1, reconRepo.findByWarehouseCode("WH-TGT").size(), "WH-TGT 盘点单应 1 条");
    }

    // =================== (4) 序列号 + 批次主数据 CRUD ===================
    @Test
    void stk4_serial_and_batch_master() {
        // 序列号 CRUD
        StkSerialNo sn1 = new StkSerialNo();
        sn1.setSerialNo("SN-T-001");
        sn1.setItemCode("ITEM-A");
        sn1.setItemName("测试物料A");
        sn1.setBatchNo("BATCH-T-001");
        sn1.setWarehouseCode("WH-TGT");
        sn1.setWarehouseName("目标仓");
        sn1.setWarrantyDate(LocalDate.of(2027, 12, 31));
        sn1.setPurchaseCost(bd("100.00"));
        sn1.setStatus(EnableStatus.ENABLED.code);
        serialRepo.save(sn1);

        StkSerialNo sn2 = new StkSerialNo();
        sn2.setSerialNo("SN-T-002");
        sn2.setItemCode("ITEM-A");
        sn2.setItemName("测试物料A");
        sn2.setBatchNo("BATCH-T-001");
        sn2.setStatus(EnableStatus.ENABLED.code);
        serialRepo.save(sn2);

        assertEquals(2, serialRepo.findByItemCode("ITEM-A").size(), "ITEM-A 序列号应 2 条");
        assertEquals(2, serialRepo.findByBatchNo("BATCH-T-001").size(), "BATCH-T-001 序列号应 2 条");
        assertTrue(serialRepo.findBySerialNo("SN-T-001").isPresent(), "SN-T-001 应存在");

        // 停用 sn2
        sn2.setStatus(EnableStatus.DISABLED.code);
        serialRepo.save(sn2);
        assertEquals(1, serialRepo.findByStatus(EnableStatus.ENABLED.code).size(), "启用序列号应 1 条");
        assertEquals(1, serialRepo.findByStatus(EnableStatus.DISABLED.code).size(), "停用序列号应 1 条");

        // 批次 CRUD
        StkBatch b1 = new StkBatch();
        b1.setBatchNo("BATCH-T-001");
        b1.setItemCode("ITEM-A");
        b1.setItemName("测试物料A");
        b1.setManufacturingDate(LocalDate.of(2026, 1, 1));
        b1.setExpiryDate(LocalDate.of(2027, 12, 31));
        b1.setInitialQty(bd("1000"));
        b1.setSupplierCode("SUP-001");
        b1.setStatus(EnableStatus.ENABLED.code);
        batchRepo.save(b1);

        assertTrue(batchRepo.findByBatchNo("BATCH-T-001").isPresent(), "BATCH-T-001 应存在");
        assertEquals(1, batchRepo.findByItemCode("ITEM-A").size(), "ITEM-A 批次应 1 条");
        assertEquals(1, batchRepo.findByStatus(EnableStatus.ENABLED.code).size(), "启用批次应 1 条");
    }

    // =================== (5) 状态机守卫 ===================
    @Test
    void stk5_state_machine_guard() {
        // (a) DataProxy beforeUpdate:status 禁止表单直改(非草稿抛异常)
        StkStockEntry.Proxy entryProxy = new StkStockEntry.Proxy();
        StkStockEntry draftE = new StkStockEntry();
        draftE.setStatus(EntryStatus.DRAFT.code);
        assertDoesNotThrow(() -> entryProxy.beforeUpdate(draftE), "草稿状态允许表单编辑");
        StkStockEntry subE = new StkStockEntry();
        subE.setStatus(EntryStatus.SUBMITTED.code);
        assertThrows(IllegalArgumentException.class, () -> entryProxy.beforeUpdate(subE),
            "已提交状态修改应抛异常:status 禁止表单直改");

        StkStockReconciliation.Proxy reconProxy = new StkStockReconciliation.Proxy();
        StkStockReconciliation subR = new StkStockReconciliation();
        subR.setStatus(ReconciliationStatus.SUBMITTED.code);
        assertThrows(IllegalArgumentException.class, () -> reconProxy.beforeUpdate(subR),
            "盘点单已提交状态修改应抛异常");

        // (b) 已取消不可再取消(出入库单)
        StkStockEntry e = buildEntry("SE-GUARD-001", EntryType.MATERIAL_RECEIPT.code,
            null, "WH-TGT");
        entryLifecycle.exec(List.of(e), null, new String[]{StkEntryLifecycleHandler.CODE_CANCEL});
        StkStockEntry cancelled = entryRepo.findById(e.getId()).orElseThrow();
        String reCancel = entryLifecycle.exec(List.of(cancelled), null,
            new String[]{StkEntryLifecycleHandler.CODE_CANCEL});
        assertTrue(reCancel.contains("失败 1"), "已取消再取消应被拒,实际=" + reCancel);

        // (c) 非草稿不可提交(出入库单)
        StkStockEntry e2 = buildEntry("SE-GUARD-002", EntryType.MATERIAL_RECEIPT.code,
            null, "WH-TGT");
        entryLifecycle.exec(List.of(e2), null, new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        StkStockEntry submitted = entryRepo.findById(e2.getId()).orElseThrow();
        String reSubmit = entryLifecycle.exec(List.of(submitted), null,
            new String[]{StkEntryLifecycleHandler.CODE_SUBMIT});
        assertTrue(reSubmit.contains("失败 1"), "已提交再提交应被拒,实际=" + reSubmit);

        // (d) 盘点单已取消不可再取消
        StkStockReconciliation r = new StkStockReconciliation();
        r.setReconciliationNo("SR-GUARD-001");
        r.setWarehouseCode("WH-TGT");
        r.setReconcileDate(LocalDate.now());
        r.setStatus(ReconciliationStatus.DRAFT.code);
        StkReconciliationItem it = new StkReconciliationItem();
        it.setReconciliation(r);
        it.setItemCode("ITEM-A");
        it.setItemName("测试物料A");
        it.setBookQty(bd("100"));
        it.setActualQty(bd("100"));
        r.setItems(new ArrayList<>(List.of(it)));
        reconRepo.save(r);
        reconLifecycle.exec(List.of(r), null,
            new String[]{StkReconciliationLifecycleHandler.CODE_CANCEL});
        StkStockReconciliation rCancelled = reconRepo.findById(r.getId()).orElseThrow();
        String rReCancel = reconLifecycle.exec(List.of(rCancelled), null,
            new String[]{StkReconciliationLifecycleHandler.CODE_CANCEL});
        assertTrue(rReCancel.contains("失败 1"), "盘点单已取消再取消应被拒,实际=" + rReCancel);
    }

    // =================== (6) 库存配置单例(Initializer 启动幂等) ===================
    @Test
    void stk6_stock_settings_singleton() {
        // Initializer 在 ApplicationReadyEvent 已幂等插入 1 条默认配置
        assertEquals(1, settingsRepo.count(), "单例配置应仅 1 条(Initializer 幂等)");

        StkStockSettings got = settingsRepo.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals("WH-DEFAULT", got.getDefaultWarehouseCode(), "默认仓库编码=Initializer 默认值");
        assertEquals("默认仓库", got.getDefaultWarehouseName(), "默认仓库名称=默认值");
        assertFalse(got.getBatchEnabled(), "批次管理默认关闭");
        assertFalse(got.getSerialNoEnabled(), "序列号管理默认关闭");
        assertEquals(90, got.getStockAgeWarningDays(), "库龄预警天数=90");
        assertEquals(EnableStatus.ENABLED.code, got.getStatus(), "状态=启用");

        // 修改单例配置 + 回读验证
        got.setDefaultWarehouseCode("WH-ADJUSTED");
        got.setBatchEnabled(true);
        got.setStockAgeWarningDays(180);
        settingsRepo.save(got);
        StkStockSettings reread = settingsRepo.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals("WH-ADJUSTED", reread.getDefaultWarehouseCode(), "修改后仓库编码应回读");
        assertTrue(reread.getBatchEnabled(), "修改后批次管理=启用");
        assertEquals(180, reread.getStockAgeWarningDays(), "修改后库龄预警=180");
        assertEquals(1, settingsRepo.count(), "修改不应新增记录,仍仅 1 条");
    }
}
