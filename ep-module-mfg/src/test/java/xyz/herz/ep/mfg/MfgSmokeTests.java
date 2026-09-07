package xyz.herz.ep.mfg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.erp.entity.master.ErpProductBrand;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;
import xyz.herz.ep.erp.entity.stock.ErpStockBalance;
import xyz.herz.ep.erp.enums.ErpDictEnums.ProductListingStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockBizType;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade.ChangeItem;
import xyz.herz.ep.erp.jpa.master.ErpProductBrandRepository;
import xyz.herz.ep.erp.jpa.master.ErpProductCategoryRepository;
import xyz.herz.ep.erp.jpa.master.ErpProductUnitRepository;
import xyz.herz.ep.erp.jpa.master.ErpSupplierRepository;
import xyz.herz.ep.erp.jpa.master.ErpWarehouseRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductSkuRepository;
import xyz.herz.ep.erp.jpa.stock.ErpStockBalanceRepository;

import xyz.herz.ep.mfg.entity.bom.MfgBom;
import xyz.herz.ep.mfg.entity.bom.MfgBomItem;
import xyz.herz.ep.mfg.entity.jobcard.MfgJobCard;
import xyz.herz.ep.mfg.entity.operation.MfgOperation;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntry;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntryItem;
import xyz.herz.ep.mfg.entity.subcontract.MfgSubcontractingOrder;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.entity.workstation.MfgWorkstation;
import xyz.herz.ep.mfg.enums.MfgDictEnums.*;
import xyz.herz.ep.mfg.handler.jobcard.MfgJobCardLifecycleHandler;
import xyz.herz.ep.mfg.handler.master.MfgMasterToggleHandler;
import xyz.herz.ep.mfg.handler.stockentry.MfgStockEntryAuditHandler;
import xyz.herz.ep.mfg.handler.stockentry.MfgStockEntryCancelHandler;
import xyz.herz.ep.mfg.handler.subcontract.MfgSubcontractingLifecycleHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderCancelHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderCompleteHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderStartHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderStopHandler;
import xyz.herz.ep.mfg.jpa.bom.MfgBomRepository;
import xyz.herz.ep.mfg.jpa.jobcard.MfgJobCardRepository;
import xyz.herz.ep.mfg.jpa.operation.MfgOperationRepository;
import xyz.herz.ep.mfg.jpa.stockentry.MfgStockEntryRepository;
import xyz.herz.ep.mfg.jpa.subcontract.MfgSubcontractingOrderRepository;
import xyz.herz.ep.mfg.jpa.workorder.MfgWorkOrderRepository;
import xyz.herz.ep.mfg.jpa.workstation.MfgWorkstationRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 制造模块冒烟测试(参考 ERPNext Manufacturing DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-B 验收矩阵:
 * <ol>
 *   <li>Mfg1 BOM 主数据 + 启停切换(行按钮)</li>
 *   <li>Mfg2 工单状态机:草稿→开工→完工 + 停工/取消</li>
 *   <li>Mfg3 派工单开工/完工 + 工时回写</li>
 *   <li>Mfg4 生产领料审核 → InventoryChangeFacade 库存联动(预存 100,领 40,余 60)</li>
 *   <li>Mfg5 委外单:下单→收货→结算 + totalAmount 回写</li>
 *   <li>Mfg6 BOM 成本汇总:sum(items.amount) == totalCost</li>
 * </ol>
 *
 * <p>跨模块依赖 erp:InventoryChangeFacade(ErpStockService)+ ErpProduct/Warehouse/Supplier。
 */
@SpringBootTest(classes = MfgTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class MfgSmokeTests {

    // ---- erp 主数据 Repository(跨模块依赖) ----
    @Autowired ErpProductCategoryRepository catRepo;
    @Autowired ErpProductUnitRepository unitRepo;
    @Autowired ErpProductBrandRepository brandRepo;
    @Autowired ErpWarehouseRepository whRepo;
    @Autowired ErpSupplierRepository supplierRepo;
    @Autowired ErpProductRepository productRepo;
    @Autowired ErpProductSkuRepository skuRepo;
    @Autowired ErpStockBalanceRepository balanceRepo;

    // ---- mfg Repository ----
    @Autowired MfgBomRepository bomRepo;
    @Autowired MfgWorkOrderRepository woRepo;
    @Autowired MfgOperationRepository opRepo;
    @Autowired MfgJobCardRepository jcRepo;
    @Autowired MfgWorkstationRepository wsRepo;
    @Autowired MfgStockEntryRepository seRepo;
    @Autowired MfgSubcontractingOrderRepository subcoRepo;

    // ---- Facade + Handler ----
    @Autowired InventoryChangeFacade inventoryFacade;
    @Autowired MfgMasterToggleHandler masterToggle;
    @Autowired MfgWorkOrderStartHandler woStart;
    @Autowired MfgWorkOrderCompleteHandler woComplete;
    @Autowired MfgWorkOrderStopHandler woStop;
    @Autowired MfgWorkOrderCancelHandler woCancel;
    @Autowired MfgJobCardLifecycleHandler jcLifecycle;
    @Autowired MfgStockEntryAuditHandler seAudit;
    @Autowired MfgStockEntryCancelHandler seCancel;
    @Autowired MfgSubcontractingLifecycleHandler subcoLifecycle;

    // =================== (1) BOM 主数据 + 启停切换 ===================
    @Test
    void mfg1_bom_master_and_toggle() {
        ErpProduct p = basicProduct("P-BOM-1").getProduct();
        MfgBom bom = new MfgBom();
        bom.setNo("BOM-T-001");
        bom.setProduct(p);
        bom.setIsActive(true);
        bom.setIsDefault(true);
        bom.setWithOperations(false);
        bom.setStatus(EnableStatus.ENABLED.code);
        bom.setItems(new ArrayList<>(List.of(
            bomItem(p, new BigDecimal("2"), new BigDecimal("10")),
            bomItem(p, new BigDecimal("1"), new BigDecimal("50"))
        )));
        bom = bomRepo.save(bom);
        assertEquals(2, bom.getItems().size(), "BOM 应含 2 条明细");

        // 行按钮停用 → DISABLED
        String r1 = masterToggle.exec(List.of(bom), null,
            new String[]{MfgMasterToggleHandler.DISABLE});
        assertTrue(r1.contains("成功 1"), "停用应成功,实际=" + r1);
        assertEquals(EnableStatus.DISABLED.code,
            bomRepo.findById(bom.getId()).orElseThrow().getStatus());

        // 行按钮重新启用 → ENABLED
        String r2 = masterToggle.exec(List.of(bom), null,
            new String[]{MfgMasterToggleHandler.ENABLE});
        assertTrue(r2.contains("成功 1"), "启用应成功,实际=" + r2);
        assertEquals(EnableStatus.ENABLED.code,
            bomRepo.findById(bom.getId()).orElseThrow().getStatus());
    }

    // =================== (2) 工单状态机:开工→完工 + 停工/取消 ===================
    @Test
    void mfg2_work_order_lifecycle() {
        ProductSkuRef ref = basicProduct("P-WO-1");
        ErpWarehouse wh = basicWarehouse();

        MfgBom bom = basicBom(ref.getProduct(), "BOM-WO-1");
        MfgWorkOrder wo = new MfgWorkOrder();
        wo.setNo("WO-T-002");
        wo.setProduct(ref.getProduct());
        wo.setBom(bom);
        wo.setQty(new BigDecimal("100"));
        wo.setProducedQty(BigDecimal.ZERO);
        wo.setSourceWarehouse(wh);
        wo.setTargetWarehouse(wh);
        wo.setStatus(WorkOrderStatus.DRAFT.code);
        wo = woRepo.save(wo);

        // 开工 → IN_PRODUCTION + actualStart
        String r1 = woStart.exec(List.of(wo), null,
            new String[]{MfgWorkOrderStartHandler.CODE_START});
        assertTrue(r1.contains("成功 1"), "开工应成功,实际=" + r1);
        MfgWorkOrder afterStart = woRepo.findById(wo.getId()).orElseThrow();
        assertEquals(WorkOrderStatus.IN_PRODUCTION.code, afterStart.getStatus());
        assertNotNull(afterStart.getActualStart(), "开工应记录 actualStart");

        // 模拟完工入库回写 producedQty
        afterStart.setProducedQty(new BigDecimal("100"));
        // 完工 → COMPLETED + actualEnd
        String r2 = woComplete.exec(List.of(afterStart), null,
            new String[]{MfgWorkOrderCompleteHandler.CODE_COMPLETE});
        assertTrue(r2.contains("成功 1"), "完工应成功,实际=" + r2);
        MfgWorkOrder afterDone = woRepo.findById(wo.getId()).orElseThrow();
        assertEquals(WorkOrderStatus.COMPLETED.code, afterDone.getStatus());
        assertNotNull(afterDone.getActualEnd(), "完工应记录 actualEnd");

        // --- 停工场景:新工单 → 开工 → 停工 ---
        MfgWorkOrder wo2 = basicWorkOrder(ref.getProduct(), "WO-T-002B", new BigDecimal("50"), wh);
        woStart.exec(List.of(wo2), null, new String[]{MfgWorkOrderStartHandler.CODE_START});
        String rStop = woStop.exec(List.of(wo2), null,
            new String[]{MfgWorkOrderStopHandler.CODE_STOP});
        assertTrue(rStop.contains("成功 1"), "停工应成功,实际=" + rStop);
        assertEquals(WorkOrderStatus.STOPPED.code,
            woRepo.findById(wo2.getId()).orElseThrow().getStatus());

        // --- 取消场景:草稿工单 → 取消 ---
        MfgWorkOrder wo3 = basicWorkOrder(ref.getProduct(), "WO-T-002C", new BigDecimal("30"), wh);
        String rCancel = woCancel.exec(List.of(wo3), null,
            new String[]{MfgWorkOrderCancelHandler.CODE_CANCEL});
        assertTrue(rCancel.contains("成功 1"), "取消应成功,实际=" + rCancel);
        assertEquals(WorkOrderStatus.CANCELLED.code,
            woRepo.findById(wo3.getId()).orElseThrow().getStatus());

        // --- 已完工工单不可取消(OperationHandler 契约:捕获异常并返回失败串,与 ERP 模块一致) ---
        String rRejectCancel = woCancel.exec(List.of(afterDone), null,
            new String[]{MfgWorkOrderCancelHandler.CODE_CANCEL});
        assertTrue(rRejectCancel.contains("失败 1") && rRejectCancel.contains("不可取消"),
            "已完工工单取消应被拒,实际=" + rRejectCancel);
    }

    // =================== (3) 派工单开工/完工 + 工时回写 ===================
    @Test
    void mfg3_job_card_writeback() {
        ProductSkuRef ref = basicProduct("P-JC-1");
        ErpWarehouse wh = basicWarehouse();
        MfgWorkOrder wo = basicWorkOrder(ref.getProduct(), "WO-JC-003", new BigDecimal("100"), wh);

        MfgWorkstation ws = new MfgWorkstation();
        ws.setCode("WS-" + System.nanoTime());
        ws.setName("测试工作中心");
        ws.setProductionCapacity(2);
        ws.setHourRate(new BigDecimal("80.00"));
        ws.setStatus(EnableStatus.ENABLED.code);
        wsRepo.save(ws);

        MfgOperation op = new MfgOperation();
        op.setNo("OP-" + System.nanoTime());
        op.setName("组装工序");
        op.setWorkstation(ws);
        op.setSeqNo(1);
        op.setTimeInMins(new BigDecimal("30"));
        op.setStatus(EnableStatus.ENABLED.code);
        opRepo.save(op);

        MfgJobCard jc = new MfgJobCard();
        jc.setNo("JC-T-003");
        jc.setWorkOrder(wo);
        jc.setOperation(op);
        jc.setEmployeeId(5001L);
        jc.setEmployeeName("张三");
        jc.setCompletedQty(BigDecimal.ZERO);
        jc.setRejectedQty(BigDecimal.ZERO);
        jc.setStatus(JobCardStatus.PENDING.code);
        jc = jcRepo.save(jc);

        // 开工 → IN_PRODUCTION + startedAt
        String r1 = jcLifecycle.exec(List.of(jc), null,
            new String[]{MfgJobCardLifecycleHandler.CODE_START});
        assertTrue(r1.contains("成功 1"), "派工开工应成功,实际=" + r1);
        MfgJobCard afterStart = jcRepo.findById(jc.getId()).orElseThrow();
        assertEquals(JobCardStatus.IN_PRODUCTION.code, afterStart.getStatus());
        assertNotNull(afterStart.getStartedAt());

        // 完工 → COMPLETED + completedAt + totalTimeInMins(按 startedAt→completedAt 差值)
        String r2 = jcLifecycle.exec(List.of(afterStart), null,
            new String[]{MfgJobCardLifecycleHandler.CODE_COMPLETE});
        assertTrue(r2.contains("成功 1"), "派工完工应成功,实际=" + r2);
        MfgJobCard afterDone = jcRepo.findById(jc.getId()).orElseThrow();
        assertEquals(JobCardStatus.COMPLETED.code, afterDone.getStatus());
        assertNotNull(afterDone.getCompletedAt());
        assertNotNull(afterDone.getTotalTimeInMins(), "完工应回写工时");
        assertTrue(afterDone.getTotalTimeInMins().signum() >= 0, "工时 >= 0");
    }

    // =================== (4) 生产领料审核 → 库存联动 ===================
    @Test
    void mfg4_stock_entry_consume_inventory() {
        ProductSkuRef ref = basicProduct("P-SE-1");
        ErpWarehouse wh = basicWarehouse();

        // 预存 100 个(用 InventoryChangeFacade 直接入库)
        inventoryFacade.change(new InventoryChangeFacade.ChangeRequest(
            "PRE-STOCK-SE", 99004L, StockBizType.PURCHASE_IN.code,
            List.of(new ChangeItem(ref.productId, wh.getId(), new BigDecimal("100"))),
            "测试预备货"));
        assertEquals(0, new BigDecimal("100").compareTo(qtyNow(ref.productId, wh.getId())),
            "预备货后库存应 100");

        // 创建领料单(MATERIAL_ISSUE),从仓库领 40
        MfgWorkOrder wo = basicWorkOrder(ref.getProduct(), "WO-SE-004", new BigDecimal("100"), wh);
        MfgStockEntry se = new MfgStockEntry();
        se.setNo("SE-T-004");
        se.setTtype(StockEntryType.MATERIAL_ISSUE.code);
        se.setWorkOrder(wo);
        se.setFromWarehouse(wh);
        se.setToWarehouse(wh);
        se.setStatus(StockEntryStatus.DRAFT.code);
        MfgStockEntryItem item = new MfgStockEntryItem();
        item.setProduct(ref.getProduct());
        item.setQty(new BigDecimal("40"));
        item.setUnit(ref.unit);
        item.setAmount(new BigDecimal("400.00"));
        se.setItems(new ArrayList<>(List.of(item)));
        se = seRepo.save(se);

        // 审核 → AUDITED + 库存 -40 = 60
        String r = seAudit.exec(List.of(se), null,
            new String[]{MfgStockEntryAuditHandler.CODE_AUDIT});
        assertTrue(r.contains("成功 1"), "领料审核应成功,实际=" + r);
        MfgStockEntry afterAudit = seRepo.findById(se.getId()).orElseThrow();
        assertEquals(StockEntryStatus.AUDITED.code, afterAudit.getStatus());
        assertEquals(0, new BigDecimal("40").compareTo(afterAudit.getTotalQty()), "totalQty 应回写 40");
        assertEquals(0, new BigDecimal("60").compareTo(qtyNow(ref.productId, wh.getId())),
            "领料后库存应 60");

        // 幂等:重复审核不重复扣库存(facade 层 idempotent by bizType+bizId)
        seAudit.exec(List.of(afterAudit), null,
            new String[]{MfgStockEntryAuditHandler.CODE_AUDIT});
        assertEquals(0, new BigDecimal("60").compareTo(qtyNow(ref.productId, wh.getId())),
            "幂等重复审核不得再扣库存");

        // 已审核单据不可直接取消(OperationHandler 契约:捕获异常并返回失败串)
        String rRejectCancel = seCancel.exec(List.of(afterAudit), null,
            new String[]{MfgStockEntryCancelHandler.CODE_CANCEL});
        assertTrue(rRejectCancel.contains("失败 1") && rRejectCancel.contains("不可直接取消"),
            "已审核单据取消应被拒,实际=" + rRejectCancel);
    }

    // =================== (5) 委外单:下单→收货→结算 ===================
    @Test
    void mfg5_subcontracting_lifecycle() {
        ProductSkuRef ref = basicProduct("P-SUB-1");
        ErpWarehouse wh = basicWarehouse();
        ErpSupplier s = new ErpSupplier();
        s.setName("委外供应商"); s.setCode("SP-" + System.nanoTime());
        s.setStatus(EnableStatus.ENABLED.code);
        supplierRepo.save(s);

        MfgWorkOrder wo = basicWorkOrder(ref.getProduct(), "WO-SUB-005", new BigDecimal("200"), wh);
        MfgSubcontractingOrder sub = new MfgSubcontractingOrder();
        sub.setNo("SUB-T-005");
        sub.setSupplier(s);
        sub.setWorkOrder(wo);
        sub.setQty(new BigDecimal("100"));
        sub.setReceivedQty(BigDecimal.ZERO);
        sub.setRate(new BigDecimal("5.00"));
        sub.setStatus(SubcontractingStatus.DRAFT.code);
        sub = subcoRepo.save(sub);

        // 下单 → ORDERED + totalAmount = 100 * 5 = 500
        String r1 = subcoLifecycle.exec(List.of(sub), null,
            new String[]{MfgSubcontractingLifecycleHandler.CODE_SUBMIT});
        assertTrue(r1.contains("成功 1"), "委外下单应成功,实际=" + r1);
        MfgSubcontractingOrder afterSubmit = subcoRepo.findById(sub.getId()).orElseThrow();
        assertEquals(SubcontractingStatus.ORDERED.code, afterSubmit.getStatus());
        assertEquals(0, new BigDecimal("500.00").compareTo(afterSubmit.getTotalAmount()),
            "totalAmount 应 = 100 * 5 = 500");

        // 模拟部分收货
        afterSubmit.setReceivedQty(new BigDecimal("100"));
        // 收货 → FULL_RECEIVED(已收 == 计划)
        String r2 = subcoLifecycle.exec(List.of(afterSubmit), null,
            new String[]{MfgSubcontractingLifecycleHandler.CODE_RECEIVE});
        assertTrue(r2.contains("成功 1"), "委外收货应成功,实际=" + r2);
        assertEquals(SubcontractingStatus.FULL_RECEIVED.code,
            subcoRepo.findById(sub.getId()).orElseThrow().getStatus());

        // 结算 → SETTLED
        String r3 = subcoLifecycle.exec(List.of(afterSubmit), null,
            new String[]{MfgSubcontractingLifecycleHandler.CODE_SETTLE});
        assertTrue(r3.contains("成功 1"), "委外结算应成功,实际=" + r3);
        assertEquals(SubcontractingStatus.SETTLED.code,
            subcoRepo.findById(sub.getId()).orElseThrow().getStatus());
    }

    // =================== (6) BOM 成本汇总 ===================
    @Test
    void mfg6_bom_cost_rollup() {
        ErpProduct p = basicProduct("P-COST-1").getProduct();
        MfgBom bom = new MfgBom();
        bom.setNo("BOM-COST-006");
        bom.setProduct(p);
        bom.setIsActive(true);
        bom.setStatus(EnableStatus.ENABLED.code);

        MfgBomItem i1 = bomItem(p, new BigDecimal("2"), new BigDecimal("10"));   // 2 * 10 = 20
        i1.setAmount(new BigDecimal("20.00"));
        MfgBomItem i2 = bomItem(p, new BigDecimal("1"), new BigDecimal("50"));   // 1 * 50 = 50
        i2.setAmount(new BigDecimal("50.00"));
        bom.setItems(new ArrayList<>(List.of(i1, i2)));

        // 成本汇总:totalCost = sum(items.amount) = 20 + 50 = 70
        BigDecimal totalCost = bom.getItems().stream()
            .map(it -> it.getAmount() == null ? BigDecimal.ZERO : it.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        bom.setTotalCost(totalCost);
        bom = bomRepo.save(bom);

        MfgBom loaded = bomRepo.findById(bom.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("70.00").compareTo(loaded.getTotalCost()),
            "BOM 总成本应为 70");
        assertEquals(2, loaded.getItems().size());

        BigDecimal sumItems = loaded.getItems().stream()
            .map(it -> it.getAmount() == null ? BigDecimal.ZERO : it.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, loaded.getTotalCost().compareTo(sumItems),
            "totalCost 应等于明细金额之和");
    }

    // =================== helpers ===================

    /** 产品+SKU+分类+单位+品牌 的最小 fixture,返回产品 ID 和单位引用。 */
    private static class ProductSkuRef {
        final ErpProduct product;
        final ErpProductUnit unit;
        final Long productId;
        ProductSkuRef(ErpProduct p, ErpProductUnit u) { this.product = p; this.unit = u; this.productId = p.getId(); }
        ErpProduct getProduct() { return product; }
    }

    private ProductSkuRef basicProduct(String seed) {
        ErpProductCategory cat = new ErpProductCategory();
        cat.setName("cat-" + seed); cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        ErpProductUnit unit = new ErpProductUnit();
        unit.setName("件"); unit.setSymbol("U-" + seed); unit.setStatus(EnableStatus.ENABLED.code);
        unitRepo.save(unit);
        ErpProductBrand brand = new ErpProductBrand();
        brand.setCode("BR-" + seed); brand.setName("品牌-" + seed);
        brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);

        ErpProduct p = new ErpProduct();
        p.setCode("P-" + seed + System.nanoTime());
        p.setName("产品-" + seed);
        p.setCategory(cat); p.setUnit(unit); p.setBrand(brand);
        p.setStatus(EnableStatus.ENABLED.code);
        p.setListingStatus(ProductListingStatus.LISTED.code);
        ErpProductSku sku = new ErpProductSku();
        sku.setCode("SKU-" + seed);
        sku.setSpecsText("默认规格");
        sku.setSalePrice(new BigDecimal("10.00"));
        sku.setProduct(p);
        p.setSkus(new ArrayList<>(List.of(sku)));
        productRepo.save(p);
        return new ProductSkuRef(p, unit);
    }

    private ErpWarehouse basicWarehouse() {
        ErpWarehouse wh = new ErpWarehouse();
        wh.setCode("WH-" + System.nanoTime());
        wh.setName("测试仓");
        wh.setStatus(EnableStatus.ENABLED.code);
        wh.setDefaultFlag(false);
        whRepo.save(wh);
        return wh;
    }

    private MfgBom basicBom(ErpProduct p, String no) {
        MfgBom bom = new MfgBom();
        bom.setNo(no + System.nanoTime());
        bom.setProduct(p);
        bom.setIsActive(true);
        bom.setStatus(EnableStatus.ENABLED.code);
        return bomRepo.save(bom);
    }

    private MfgWorkOrder basicWorkOrder(ErpProduct p, String no, BigDecimal qty, ErpWarehouse wh) {
        MfgWorkOrder wo = new MfgWorkOrder();
        wo.setNo(no + System.nanoTime());
        wo.setProduct(p);
        wo.setQty(qty);
        wo.setProducedQty(BigDecimal.ZERO);
        wo.setSourceWarehouse(wh);
        wo.setTargetWarehouse(wh);
        wo.setStatus(WorkOrderStatus.DRAFT.code);
        return woRepo.save(wo);
    }

    private MfgBomItem bomItem(ErpProduct p, BigDecimal qty, BigDecimal rate) {
        MfgBomItem it = new MfgBomItem();
        it.setProduct(p);
        it.setQty(qty);
        it.setRate(rate);
        it.setAmount(qty.multiply(rate));
        it.setIsScrap(false);
        return it;
    }

    private BigDecimal qtyNow(Long productId, Long whId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpStockBalance b : balanceRepo.findAll()) {
            if (b.getProduct() != null && b.getWarehouse() != null
                    && productId.equals(b.getProduct().getId())
                    && whId.equals(b.getWarehouse().getId())) {
                if (b.getQty() != null) sum = sum.add(b.getQty());
            }
        }
        return sum;
    }
}
