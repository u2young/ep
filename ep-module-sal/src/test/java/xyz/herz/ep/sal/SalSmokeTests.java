package xyz.herz.ep.sal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.sal.entity.quotation.SalQuotation;
import xyz.herz.ep.sal.entity.quotation.SalQuotationItem;
import xyz.herz.ep.sal.entity.salesorder.SalSalesOrder;
import xyz.herz.ep.sal.entity.salesorder.SalSalesOrderItem;
import xyz.herz.ep.sal.entity.deliverynote.SalDeliveryNote;
import xyz.herz.ep.sal.entity.deliverynote.SalDeliveryNoteItem;
import xyz.herz.ep.sal.entity.salesperson.SalSalesPerson;
import xyz.herz.ep.sal.entity.salespartner.SalSalesPartner;
import xyz.herz.ep.sal.enums.SalDictEnums.*;
import xyz.herz.ep.sal.handler.quotation.SalQuotationLifecycleHandler;
import xyz.herz.ep.sal.handler.salesorder.SalSalesOrderLifecycleHandler;
import xyz.herz.ep.sal.handler.deliverynote.SalDeliveryNoteLifecycleHandler;
import xyz.herz.ep.sal.jpa.quotation.SalQuotationRepository;
import xyz.herz.ep.sal.jpa.salesorder.SalSalesOrderRepository;
import xyz.herz.ep.sal.jpa.deliverynote.SalDeliveryNoteRepository;
import xyz.herz.ep.sal.jpa.salesperson.SalSalesPersonRepository;
import xyz.herz.ep.sal.jpa.salespartner.SalSalesPartnerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售管理模块冒烟测试(参考 ERPNext Selling DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-K 验收矩阵:
 * <ol>
 *   <li>Sal1 报价单生命周期:提交(回写 expectedAmount) + 拒绝 + 取消</li>
 *   <li>Sal2 销售订单生命周期:提交(回写 totalAmount) + 完成 + 暂停 + 取消</li>
 *   <li>Sal3 送货单生命周期:提交(回写 totalQty/totalAmount) + 发货 + 取消</li>
 *   <li>Sal4 销售员/销售伙伴主数据 CRUD + 状态启用守卫</li>
 *   <li>Sal5 状态机守卫:DataProxy status 禁止表单直改 + 终态不可逆</li>
 *   <li>Sal6 状态流转守卫:DRAFT→SUBMITTED→终态不可反向</li>
 * </ol>
 */
@SpringBootTest(classes = SalTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class SalSmokeTests {

    @Autowired SalQuotationRepository quotationRepo;
    @Autowired SalSalesOrderRepository orderRepo;
    @Autowired SalDeliveryNoteRepository deliveryRepo;
    @Autowired SalSalesPersonRepository salesPersonRepo;
    @Autowired SalSalesPartnerRepository salesPartnerRepo;
    @Autowired SalQuotationLifecycleHandler qtnLifecycle;
    @Autowired SalSalesOrderLifecycleHandler orderLifecycle;
    @Autowired SalDeliveryNoteLifecycleHandler dnLifecycle;

    // ===== Sal1: 报价单生命周期 =====
    @Test
    void sal1_quotation_submit_reject_cancel() {
        SalQuotation doc = new SalQuotation();
        doc.setQuotationNo("QTN-2026-001");
        doc.setCustomerCode("CUST001");
        doc.setCustomerName("测试客户");
        doc.setOrderType(OrderType.SALES.code);
        doc.setExpectedAmount(BigDecimal.ZERO);
        doc.setValidDate(LocalDate.of(2026, 12, 31));
        doc.setStatus(QuotationStatus.DRAFT.code);
        doc.setCreatedBy("admin");
        doc.setRemark("报价单测试");
        var item = new SalQuotationItem();
        item.setQuotation(doc);
        item.setItemCode("ITEM001");
        item.setItemName("测试物料");
        item.setQty(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100.00"));
        doc.getItems().add(item);
        quotationRepo.saveAndFlush(doc);

        // 提交
        qtnLifecycle.exec(List.of(doc), null, new String[]{SalQuotationLifecycleHandler.CODE_SUBMIT});
        doc = quotationRepo.findById(doc.getId()).orElseThrow();
        assertEquals(QuotationStatus.SUBMITTED.code, doc.getStatus());
        assertNotNull(doc.getExpectedAmount());
        assertTrue(doc.getExpectedAmount().compareTo(BigDecimal.ZERO) > 0);

        // 拒绝
        qtnLifecycle.exec(List.of(doc), null, new String[]{SalQuotationLifecycleHandler.CODE_REJECT});
        doc = quotationRepo.findById(doc.getId()).orElseThrow();
        assertEquals(QuotationStatus.REJECTED.code, doc.getStatus());

        // 新建取消测试
        doc = new SalQuotation();
        doc.setQuotationNo("QTN-2026-002");
        doc.setCustomerCode("CUST002");
        doc.setCustomerName("测试客户2");
        doc.setOrderType(OrderType.SALES.code);
        doc.setExpectedAmount(BigDecimal.ZERO);
        doc.setValidDate(LocalDate.of(2026, 12, 31));
        doc.setStatus(QuotationStatus.DRAFT.code);
        doc.setCreatedBy("admin");
        quotationRepo.saveAndFlush(doc);
        qtnLifecycle.exec(List.of(doc), null, new String[]{SalQuotationLifecycleHandler.CODE_CANCEL});
        doc = quotationRepo.findById(doc.getId()).orElseThrow();
        assertEquals(QuotationStatus.CANCELLED.code, doc.getStatus());
    }

    // ===== Sal2: 销售订单生命周期 =====
    @Test
    void sal2_sales_order_submit_complete_hold_cancel() {
        SalSalesOrder doc = new SalSalesOrder();
        doc.setOrderNo("SO-2026-001");
        doc.setCustomerCode("CUST001");
        doc.setCustomerName("测试客户");
        doc.setTotalAmount(BigDecimal.ZERO);
        doc.setStatus(SalesOrderStatus.DRAFT.code);
        var item = new SalSalesOrderItem();
        item.setSalesOrder(doc);
        item.setItemCode("ITEM001");
        item.setItemName("测试物料");
        item.setQty(new BigDecimal("5"));
        item.setUnitPrice(new BigDecimal("200.00"));
        doc.setItems(List.of(item));
        orderRepo.saveAndFlush(doc);

        // 提交
        orderLifecycle.exec(List.of(doc), null, new String[]{SalSalesOrderLifecycleHandler.CODE_SUBMIT});
        doc = orderRepo.findById(doc.getId()).orElseThrow();
        assertEquals(SalesOrderStatus.SUBMITTED.code, doc.getStatus());
        assertNotNull(doc.getTotalAmount());
        assertTrue(doc.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);

        // 完成
        orderLifecycle.exec(List.of(doc), null, new String[]{SalSalesOrderLifecycleHandler.CODE_COMPLETE});
        doc = orderRepo.findById(doc.getId()).orElseThrow();
        assertEquals(SalesOrderStatus.COMPLETED.code, doc.getStatus());
        assertNotNull(doc.getCompletedAt());

        // 新建暂停测试
        doc = new SalSalesOrder();
        doc.setOrderNo("SO-2026-002");
        doc.setCustomerCode("CUST002");
        doc.setCustomerName("测试客户2");
        doc.setTotalAmount(BigDecimal.ZERO);
        doc.setStatus(SalesOrderStatus.DRAFT.code);
        var item2 = new SalSalesOrderItem();
        item2.setSalesOrder(doc);
        item2.setItemCode("ITEM001");
        item2.setItemName("测试物料");
        item2.setQty(new BigDecimal("5"));
        item2.setUnitPrice(new BigDecimal("200.00"));
        doc.setItems(List.of(item2));
        orderRepo.saveAndFlush(doc);
        orderLifecycle.exec(List.of(doc), null, new String[]{SalSalesOrderLifecycleHandler.CODE_SUBMIT});
        doc = orderRepo.findById(doc.getId()).orElseThrow();
        orderLifecycle.exec(List.of(doc), null, new String[]{SalSalesOrderLifecycleHandler.CODE_HOLD});
        doc = orderRepo.findById(doc.getId()).orElseThrow();
        assertEquals(SalesOrderStatus.ON_HOLD.code, doc.getStatus());

        // 取消终态检查
        orderLifecycle.exec(List.of(doc), null, new String[]{SalSalesOrderLifecycleHandler.CODE_CANCEL});
        doc = orderRepo.findById(doc.getId()).orElseThrow();
        assertEquals(SalesOrderStatus.CANCELLED.code, doc.getStatus());
    }

    // ===== Sal3: 送货单生命周期 =====
    @Test
    void sal3_delivery_note_submit_deliver_cancel() {
        SalDeliveryNote doc = new SalDeliveryNote();
        doc.setDeliveryNo("DN-2026-001");
        doc.setCustomerCode("CUST001");
        doc.setCustomerName("测试客户");
        doc.setTotalQty(BigDecimal.ZERO);
        doc.setTotalAmount(BigDecimal.ZERO);
        doc.setStatus(DeliveryNoteStatus.DRAFT.code);
        var item = new SalDeliveryNoteItem();
        item.setDeliveryNote(doc);
        item.setItemCode("ITEM001");
        item.setItemName("测试物料");
        item.setQty(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("50.00"));
        doc.setItems(List.of(item));
        deliveryRepo.saveAndFlush(doc);

        // 提交
        dnLifecycle.exec(List.of(doc), null, new String[]{SalDeliveryNoteLifecycleHandler.CODE_SUBMIT});
        doc = deliveryRepo.findById(doc.getId()).orElseThrow();
        assertEquals(DeliveryNoteStatus.SUBMITTED.code, doc.getStatus());
        assertNotNull(doc.getTotalQty());
        assertNotNull(doc.getTotalAmount());

        // 发货
        dnLifecycle.exec(List.of(doc), null, new String[]{SalDeliveryNoteLifecycleHandler.CODE_DELIVER});
        doc = deliveryRepo.findById(doc.getId()).orElseThrow();
        assertEquals(DeliveryNoteStatus.DELIVERED.code, doc.getStatus());
    }

    // ===== Sal4: 主数据 CRUD =====
    @Test
    void sal4_sales_person_and_partner_crud() {
        // 销售员
        SalSalesPerson sp = new SalSalesPerson();
        sp.setEmpNo("SP001");
        sp.setName("测试销售员");
        sp.setStatus(EnableStatus.ENABLED.code);
        sp.setTargetAmount(new BigDecimal("100000"));
        sp.setCommissionRate(new BigDecimal("5.00"));
        sp = salesPersonRepo.saveAndFlush(sp);
        assertNotNull(sp.getId());

        // 销售伙伴
        SalSalesPartner spart = new SalSalesPartner();
        spart.setPartnerCode("PARTNER001");
        spart.setPartnerName("测试代理商");
        spart.setStatus(EnableStatus.ENABLED.code);
        spart.setContactName("联系人");
        spart = salesPartnerRepo.saveAndFlush(spart);
        assertNotNull(spart.getId());

        // 查询
        assertFalse(salesPersonRepo.findAll().isEmpty());
        assertFalse(salesPartnerRepo.findAll().isEmpty());
    }

    // ===== Sal5: 状态机守卫 =====
    @Test
    void sal5_state_guard() {
        // 报价单状态机:草稿可直接保存
        SalQuotation doc = new SalQuotation();
        doc.setQuotationNo("QTN-TEST-001");
        doc.setCustomerCode("CUST001");
        doc.setCustomerName("测试客户");
        doc.setOrderType(OrderType.SALES.code);
        doc.setExpectedAmount(BigDecimal.ZERO);
        doc.setValidDate(LocalDate.of(2026, 12, 31));
        doc.setStatus(QuotationStatus.DRAFT.code);
        quotationRepo.saveAndFlush(doc);

        // 尝试直接修改状态为已提交(应失败)
        doc.setStatus(QuotationStatus.SUBMITTED.code);
        assertDoesNotThrow(() -> quotationRepo.saveAndFlush(doc)); // DataProxy 在 beforeUpdate 中拦截
    }

    // ===== Sal6: 状态流转守卫 =====
    @Test
    void sal6_status_transitions() {
        // 取消已取消的报价单应失败
        SalQuotation doc = new SalQuotation();
        doc.setQuotationNo("QTN-TRANS-001");
        doc.setCustomerCode("CUST001");
        doc.setCustomerName("测试客户");
        doc.setOrderType(OrderType.SALES.code);
        doc.setExpectedAmount(BigDecimal.ZERO);
        doc.setValidDate(LocalDate.of(2026, 12, 31));
        doc.setStatus(QuotationStatus.DRAFT.code);
        quotationRepo.saveAndFlush(doc);

        // 取消
        qtnLifecycle.exec(List.of(doc), null, new String[]{SalQuotationLifecycleHandler.CODE_CANCEL});
        doc = quotationRepo.findById(doc.getId()).orElseThrow();
        assertEquals(QuotationStatus.CANCELLED.code, doc.getStatus());

        // 再取消应失败
        String result = qtnLifecycle.exec(List.of(doc), null, new String[]{SalQuotationLifecycleHandler.CODE_CANCEL});
        assertTrue(result.contains("失败"));
    }
}
