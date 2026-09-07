package xyz.herz.ep.pur;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.pur.entity.quotation.PurSupplierQuotation;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;
import xyz.herz.ep.pur.entity.receipt.PurReceiptItem;
import xyz.herz.ep.pur.entity.requisition.PurPurchaseRequisition;
import xyz.herz.ep.pur.entity.requisition.PurRequisitionItem;
import xyz.herz.ep.pur.entity.rfq.PurRequestForQuotation;
import xyz.herz.ep.pur.entity.settings.PurPurchaseSettings;
import xyz.herz.ep.pur.enums.PurDictEnums.EnableStatus;
import xyz.herz.ep.pur.enums.PurDictEnums.Priority;
import xyz.herz.ep.pur.enums.PurDictEnums.RequisitionStatus;
import xyz.herz.ep.pur.enums.PurDictEnums.RfqStatus;
import xyz.herz.ep.pur.enums.PurDictEnums.ReceiptStatus;
import xyz.herz.ep.pur.handler.receipt.PurReceiptLifecycleHandler;
import xyz.herz.ep.pur.handler.requisition.PurRequisitionLifecycleHandler;
import xyz.herz.ep.pur.handler.rfq.PurRfqLifecycleHandler;
import xyz.herz.ep.pur.jpa.quotation.PurSupplierQuotationRepository;
import xyz.herz.ep.pur.jpa.receipt.PurPurchaseReceiptRepository;
import xyz.herz.ep.pur.jpa.requisition.PurPurchaseRequisitionRepository;
import xyz.herz.ep.pur.jpa.rfq.PurRequestForQuotationRepository;
import xyz.herz.ep.pur.jpa.settings.PurPurchaseSettingsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 采购管理模块冒烟测试(参考 ERPNext Procurement DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-I 验收矩阵:
 * <ol>
 *   <li>Pur1 请购单生命周期:提交(回写 totalAmount)→转单 + 明细小计自动计算</li>
 *   <li>Pur2 询价单生命周期:发送→收报价(receivedAt 回写) + 守卫(缺采购员/截止过期被拒)</li>
 *   <li>Pur3 采购收货单生命周期:提交(回写明细 amount)→收货(回写主单 receivedQty/Amount/At/receiver) + 守卫(未填实收被拒)</li>
 *   <li>Pur4 供应商报价 CRUD + 采纳标记 + 按询价单/是否采纳查询</li>
 *   <li>Pur5 状态机守卫:DataProxy status 禁止表单直改 + 已转单/已收货/已收报价终态不可取消</li>
 *   <li>Pur6 采购配置单例:幂等查询(findFirstByOrderByIdAsc)+ 默认值</li>
 * </ol>
 */
@SpringBootTest(classes = PurTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class PurSmokeTests {

    @Autowired PurPurchaseRequisitionRepository reqRepo;
    @Autowired PurRequestForQuotationRepository rfqRepo;
    @Autowired PurSupplierQuotationRepository quotRepo;
    @Autowired PurPurchaseReceiptRepository rcpRepo;
    @Autowired PurPurchaseSettingsRepository settingsRepo;
    @Autowired PurRequisitionLifecycleHandler reqLifecycle;
    @Autowired PurRfqLifecycleHandler rfqLifecycle;
    @Autowired PurReceiptLifecycleHandler rcpLifecycle;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private PurPurchaseRequisition buildRequisition(String no) {
        PurPurchaseRequisition req = new PurPurchaseRequisition();
        req.setRequisitionNo(no);
        req.setRequester("采购员-张三");
        req.setDepartment("采购部");
        req.setRequiredDate(LocalDate.now().plusDays(15));
        req.setPriority(Priority.HIGH.code);
        req.setStatus(RequisitionStatus.DRAFT.code);

        PurRequisitionItem i1 = new PurRequisitionItem();
        i1.setRequisition(req);
        i1.setItemCode("ITEM-A");
        i1.setItemName("测试物料A");
        i1.setQty(bd("10"));
        i1.setEstimatedPrice(bd("100.00"));
        // amount 留空,验证提交时自动计算

        PurRequisitionItem i2 = new PurRequisitionItem();
        i2.setRequisition(req);
        i2.setItemCode("ITEM-B");
        i2.setItemName("测试物料B");
        i2.setQty(bd("5"));
        i2.setEstimatedPrice(bd("200.00"));

        req.setItems(new java.util.ArrayList<>(List.of(i1, i2)));
        return reqRepo.save(req);
    }

    private PurRequestForQuotation buildRfq(String no, String buyer, LocalDate due) {
        PurRequestForQuotation rfq = new PurRequestForQuotation();
        rfq.setRfqNo(no);
        rfq.setSourceRequisitionNo("REQ-T-001");
        rfq.setBuyer(buyer);
        rfq.setDueDate(due);
        rfq.setStatus(RfqStatus.DRAFT.code);
        return rfqRepo.save(rfq);
    }

    private PurPurchaseReceipt buildReceipt(String no) {
        PurPurchaseReceipt rcp = new PurPurchaseReceipt();
        rcp.setReceiptNo(no);
        rcp.setSupplierCode("SUP-001");
        rcp.setSupplierName("测试供应商");
        rcp.setStatus(ReceiptStatus.DRAFT.code);

        PurReceiptItem i1 = new PurReceiptItem();
        i1.setReceipt(rcp);
        i1.setItemCode("ITEM-A");
        i1.setItemName("测试物料A");
        i1.setOrderedQty(bd("10"));
        i1.setReceivedQty(bd("10"));
        i1.setUnitPrice(bd("100.00"));

        PurReceiptItem i2 = new PurReceiptItem();
        i2.setReceipt(rcp);
        i2.setItemCode("ITEM-B");
        i2.setItemName("测试物料B");
        i2.setOrderedQty(bd("5"));
        i2.setReceivedQty(bd("5"));
        i2.setUnitPrice(bd("200.00"));

        rcp.setItems(new java.util.ArrayList<>(List.of(i1, i2)));
        return rcpRepo.save(rcp);
    }

    // =================== (1) 请购单生命周期 ===================
    @Test
    void pur1_requisition_lifecycle() {
        PurPurchaseRequisition req = buildRequisition("REQ-T-001");

        // 提交:回写 totalAmount + 明细 amount 自动计算
        String subR = reqLifecycle.exec(List.of(req), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_SUBMIT});
        assertTrue(subR.contains("成功 1"), "提交应成功,实际=" + subR);
        PurPurchaseRequisition submitted = reqRepo.findById(req.getId()).orElseThrow();
        assertEquals(RequisitionStatus.SUBMITTED.code, submitted.getStatus(), "状态=已提交");
        // item1 amount = 10 × 100 = 1000;item2 amount = 5 × 200 = 1000;total = 2000
        assertEquals(0, submitted.getTotalAmount().compareTo(bd("2000.00")),
            "totalAmount 应=明细合计 2000,实际=" + submitted.getTotalAmount());
        assertEquals(0, submitted.getItems().get(0).getAmount().compareTo(bd("1000.00")),
            "明细1小计=1000,实际=" + submitted.getItems().get(0).getAmount());

        // 转单:已提交 → 已转单
        String convR = reqLifecycle.exec(List.of(submitted), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_CONVERT});
        assertTrue(convR.contains("成功 1"), "转单应成功,实际=" + convR);
        PurPurchaseRequisition converted = reqRepo.findById(req.getId()).orElseThrow();
        assertEquals(RequisitionStatus.CONVERTED.code, converted.getStatus(), "状态=已转单");

        // 按状态查询
        assertEquals(1, reqRepo.findByStatus(RequisitionStatus.CONVERTED.code).size(),
            "已转单状态应 1 条");
        assertEquals(1, reqRepo.findByRequester("采购员-张三").size(),
            "采购员-张三 请购单应 1 条");
    }

    // =================== (2) 询价单生命周期 + 守卫 ===================
    @Test
    void pur2_rfq_lifecycle_and_guard() {
        PurRequestForQuotation rfq = buildRfq("RFQ-T-001", "采购员-李四",
            LocalDate.now().plusDays(7));

        // 发送
        String sendR = rfqLifecycle.exec(List.of(rfq), null,
            new String[]{PurRfqLifecycleHandler.CODE_SEND});
        assertTrue(sendR.contains("成功 1"), "发送应成功,实际=" + sendR);
        assertEquals(RfqStatus.SENT.code,
            rfqRepo.findById(rfq.getId()).orElseThrow().getStatus(), "状态=已发送");

        // 收报价:回写 receivedAt
        String recvR = rfqLifecycle.exec(List.of(rfq), null,
            new String[]{PurRfqLifecycleHandler.CODE_RECEIVE});
        assertTrue(recvR.contains("成功 1"), "收报价应成功,实际=" + recvR);
        PurRequestForQuotation received = rfqRepo.findById(rfq.getId()).orElseThrow();
        assertEquals(RfqStatus.RECEIVED.code, received.getStatus(), "状态=已收报价");
        assertNotNull(received.getReceivedAt(), "receivedAt 应回写");

        // 守卫:已收报价不可取消
        String cancelR = rfqLifecycle.exec(List.of(received), null,
            new String[]{PurRfqLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("失败 1"), "已收报价取消应被拒,实际=" + cancelR);

        // 守卫:缺采购员发送被拒
        PurRequestForQuotation noBuyer = buildRfq("RFQ-T-002", null,
            LocalDate.now().plusDays(7));
        String noBuyerR = rfqLifecycle.exec(List.of(noBuyer), null,
            new String[]{PurRfqLifecycleHandler.CODE_SEND});
        assertTrue(noBuyerR.contains("失败 1"), "缺采购员发送应被拒,实际=" + noBuyerR);

        // 守卫:截止日期过期发送被拒
        PurRequestForQuotation expired = buildRfq("RFQ-T-003", "采购员-王五",
            LocalDate.now().minusDays(1));
        String expiredR = rfqLifecycle.exec(List.of(expired), null,
            new String[]{PurRfqLifecycleHandler.CODE_SEND});
        assertTrue(expiredR.contains("失败 1"), "截止过期发送应被拒,实际=" + expiredR);
    }

    // =================== (3) 采购收货单生命周期 ===================
    @Test
    void pur3_receipt_lifecycle() {
        PurPurchaseReceipt rcp = buildReceipt("PR-T-001");

        // 提交:回写明细 amount = receivedQty × unitPrice
        String subR = rcpLifecycle.exec(List.of(rcp), null,
            new String[]{PurReceiptLifecycleHandler.CODE_SUBMIT});
        assertTrue(subR.contains("成功 1"), "提交应成功,实际=" + subR);
        PurPurchaseReceipt submitted = rcpRepo.findById(rcp.getId()).orElseThrow();
        assertEquals(ReceiptStatus.SUBMITTED.code, submitted.getStatus(), "状态=已提交");
        assertEquals(0, submitted.getItems().get(0).getAmount().compareTo(bd("1000.00")),
            "明细1小计=1000,实际=" + submitted.getItems().get(0).getAmount());

        // 收货:回写主单 receivedQty/receivedAmount/receivedAt/receiver
        String recvR = rcpLifecycle.exec(List.of(submitted), null,
            new String[]{PurReceiptLifecycleHandler.CODE_RECEIVE});
        assertTrue(recvR.contains("成功 1"), "收货应成功,实际=" + recvR);
        PurPurchaseReceipt received = rcpRepo.findById(rcp.getId()).orElseThrow();
        assertEquals(ReceiptStatus.RECEIVED.code, received.getStatus(), "状态=已收货");
        // receivedQty = 10 + 5 = 15;receivedAmount = 1000 + 1000 = 2000
        assertEquals(0, received.getReceivedQty().compareTo(bd("15")),
            "receivedQty=15,实际=" + received.getReceivedQty());
        assertEquals(0, received.getReceivedAmount().compareTo(bd("2000.00")),
            "receivedAmount=2000,实际=" + received.getReceivedAmount());
        assertNotNull(received.getReceivedAt(), "receivedAt 应回写");
        assertNotNull(received.getReceiptDate(), "receiptDate 应回写");
        assertEquals("system", received.getReceiver(), "receiver 默认=system");

        // 守卫:已收货不可取消
        String cancelR = rcpLifecycle.exec(List.of(received), null,
            new String[]{PurReceiptLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("失败 1"), "已收货取消应被拒,实际=" + cancelR);

        // 守卫:未填实收数量收货被拒
        PurPurchaseReceipt noRecv = buildReceipt("PR-T-002");
        noRecv.setStatus(ReceiptStatus.SUBMITTED.code);
        noRecv.getItems().get(0).setReceivedQty(null);
        noRecv.getItems().get(1).setReceivedQty(null);
        String noRecvR = rcpLifecycle.exec(List.of(noRecv), null,
            new String[]{PurReceiptLifecycleHandler.CODE_RECEIVE});
        assertTrue(noRecvR.contains("失败 1"), "未填实收收货应被拒,实际=" + noRecvR);

        // 按供应商/状态查询:本方法创建了 PR-T-001(已收货) 和 PR-T-002(已提交),均 SUP-001
        assertEquals(2, rcpRepo.findBySupplierCode("SUP-001").size(), "SUP-001 收货单应 2 条");
        assertEquals(1, rcpRepo.findByStatus(ReceiptStatus.RECEIVED.code).size(),
            "已收货状态应 1 条");
    }

    // =================== (4) 供应商报价 CRUD ===================
    @Test
    void pur4_supplier_quotation_crud() {
        PurSupplierQuotation q1 = new PurSupplierQuotation();
        q1.setQuotationNo("SQ-T-001");
        q1.setRfqNo("RFQ-T-001");
        q1.setSupplierCode("SUP-001");
        q1.setSupplierName("测试供应商A");
        q1.setQuotationDate(LocalDate.now());
        q1.setValidUntil(LocalDate.now().plusDays(30));
        q1.setTotalAmount(bd("1800.00"));
        q1.setIsAccepted(false);
        quotRepo.save(q1);

        PurSupplierQuotation q2 = new PurSupplierQuotation();
        q2.setQuotationNo("SQ-T-002");
        q2.setRfqNo("RFQ-T-001");
        q2.setSupplierCode("SUP-002");
        q2.setSupplierName("测试供应商B");
        q2.setQuotationDate(LocalDate.now());
        q2.setValidUntil(LocalDate.now().plusDays(30));
        q2.setTotalAmount(bd("1950.00"));
        q2.setIsAccepted(false);
        quotRepo.save(q2);

        // 按询价单查询:应 2 条
        assertEquals(2, quotRepo.findByRfqNo("RFQ-T-001").size(), "RFQ-T-001 报价应 2 条");
        assertEquals(2, quotRepo.findByIsAccepted(false).size(), "未采纳报价应 2 条");

        // 采纳最低价 q1
        q1.setIsAccepted(true);
        quotRepo.save(q1);
        assertEquals(1, quotRepo.findByIsAccepted(true).size(), "已采纳报价应 1 条");
        assertEquals(1, quotRepo.findByIsAccepted(false).size(), "未采纳报价应剩 1 条");
        assertEquals(1, quotRepo.findBySupplierCode("SUP-001").size(), "SUP-001 报价应 1 条");

        // 按单号查询
        assertTrue(quotRepo.findByQuotationNo("SQ-T-001").isPresent(), "SQ-T-001 应存在");
    }

    // =================== (5) 状态机守卫(DataProxy + 终态) ===================
    @Test
    void pur5_state_machine_guard() {
        // (a) DataProxy beforeUpdate:status 禁止表单直改(非草稿抛异常)
        PurPurchaseRequisition.Proxy reqProxy = new PurPurchaseRequisition.Proxy();
        PurPurchaseRequisition draftE = new PurPurchaseRequisition();
        draftE.setStatus(RequisitionStatus.DRAFT.code);
        assertDoesNotThrow(() -> reqProxy.beforeUpdate(draftE), "草稿状态允许表单编辑");
        PurPurchaseRequisition subE = new PurPurchaseRequisition();
        subE.setStatus(RequisitionStatus.SUBMITTED.code);
        assertThrows(IllegalArgumentException.class, () -> reqProxy.beforeUpdate(subE),
            "已提交状态修改应抛异常:status 禁止表单直改");

        PurPurchaseReceipt.Proxy rcpProxy = new PurPurchaseReceipt.Proxy();
        PurPurchaseReceipt recvE = new PurPurchaseReceipt();
        recvE.setStatus(ReceiptStatus.RECEIVED.code);
        assertThrows(IllegalArgumentException.class, () -> rcpProxy.beforeUpdate(recvE),
            "已收货状态修改应抛异常:status 禁止表单直改");

        // (b) 已转单不可取消(请购单)
        PurPurchaseRequisition req = buildRequisition("REQ-T-GUARD");
        reqLifecycle.exec(List.of(req), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_SUBMIT});
        reqLifecycle.exec(List.of(req), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_CONVERT});
        PurPurchaseRequisition converted = reqRepo.findById(req.getId()).orElseThrow();
        String cancelConv = reqLifecycle.exec(List.of(converted), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelConv.contains("失败 1"), "已转单取消应被拒,实际=" + cancelConv);

        // (c) 草稿不可直接转单(请购单须先提交)
        PurPurchaseRequisition draftReq = buildRequisition("REQ-T-DRAFT");
        String convertDraft = reqLifecycle.exec(List.of(draftReq), null,
            new String[]{PurRequisitionLifecycleHandler.CODE_CONVERT});
        assertTrue(convertDraft.contains("失败 1"), "草稿直接转单应被拒,实际=" + convertDraft);

        // (d) 已取消不可再取消(收货单)
        PurPurchaseReceipt rcp = buildReceipt("PR-T-GUARD");
        rcpLifecycle.exec(List.of(rcp), null,
            new String[]{PurReceiptLifecycleHandler.CODE_CANCEL});
        PurPurchaseReceipt cancelled = rcpRepo.findById(rcp.getId()).orElseThrow();
        String reCancel = rcpLifecycle.exec(List.of(cancelled), null,
            new String[]{PurReceiptLifecycleHandler.CODE_CANCEL});
        assertTrue(reCancel.contains("失败 1"), "已取消再取消应被拒,实际=" + reCancel);
    }

    // =================== (6) 采购配置单例(Initializer 启动幂等) ===================
    @Test
    void pur6_purchase_settings_singleton() {
        // Initializer 在 ApplicationReadyEvent 已幂等插入 1 条默认配置
        assertEquals(1, settingsRepo.count(), "单例配置应仅 1 条(Initializer 幂等)");

        PurPurchaseSettings got = settingsRepo.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals("默认采购员", got.getDefaultBuyer(), "默认采购员=Initializer 默认值");
        assertEquals(7, got.getDefaultLeadDays(), "默认前置天数=7");
        assertEquals(0, got.getMinOrderAmount().compareTo(bd("1000.00")), "最小订单金额=1000");
        assertEquals(EnableStatus.ENABLED.code, got.getStatus(), "状态=启用");

        // 修改单例配置 + 回读验证
        got.setDefaultBuyer("调整后采购员");
        got.setDefaultLeadDays(14);
        settingsRepo.save(got);
        PurPurchaseSettings reread = settingsRepo.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals("调整后采购员", reread.getDefaultBuyer(), "修改后采购员应回读");
        assertEquals(14, reread.getDefaultLeadDays(), "修改后前置天数=14");
        assertEquals(1, settingsRepo.count(), "修改不应新增记录,仍仅 1 条");
    }
}
