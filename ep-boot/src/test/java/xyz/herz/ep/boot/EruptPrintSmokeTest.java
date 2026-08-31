package xyz.herz.ep.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.enums.CrmDictEnums.ContractStatus;
import xyz.herz.ep.crm.jpa.CrmContractRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;

import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.jpa.master.ErpSupplierRepository;
import xyz.herz.ep.erp.jpa.master.ErpWarehouseRepository;
import xyz.herz.ep.erp.jpa.document.ErpPurchaseOrderRepository;

import xyz.herz.ep.wms.entity.WmsShipmentNotice;
import xyz.herz.ep.wms.entity.WmsWarehouse;
import xyz.herz.ep.wms.entity.WmsZone;
import xyz.herz.ep.wms.enums.WmsDictEnums.ShipmentNoticeStatus;
import xyz.herz.ep.wms.jpa.WmsShipmentNoticeRepository;
import xyz.herz.ep.wms.jpa.WmsWarehouseRepository;
import xyz.herz.ep.wms.jpa.WmsZoneRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 8 / AC-5 erupt-print 集成冒烟测试。
 */
@SpringBootTest(classes = EruptBusinessApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
class EruptPrintSmokeTest {

    @Autowired ApplicationContext applicationContext;

    // --- CRM fixtures ---
    @Autowired CrmCustomerRepository crmCustomerRepo;
    @Autowired CrmContractRepository crmContractRepo;

    // --- ERP fixtures ---
    @Autowired ErpSupplierRepository erpSupplierRepo;
    @Autowired ErpWarehouseRepository erpWarehouseRepo;
    @Autowired ErpPurchaseOrderRepository erpPurchaseRepo;

    // --- WMS fixtures ---
    @Autowired WmsWarehouseRepository wmsWarehouseRepo;
    @Autowired WmsZoneRepository wmsZoneRepo;
    @Autowired WmsShipmentNoticeRepository wmsShipNoticeRepo;

    // =================== (1) Initializer Bean 存在 ===================
    @Test
    void t1_print_initializer_exists() throws Exception {
        Class<?> initCls;
        try {
            initCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintInitializer");
        } catch (ClassNotFoundException e) {
            fail("缺少打印初始化器: xyz.herz.ep.boot.print.EruptPrintInitializer");
            return;
        }
        Object bean = applicationContext.getBean(initCls);
        assertNotNull(bean, "EruptPrintInitializer 应注册为 Spring Bean");
    }

    // =================== (2) 模板数 >=3 + code 抽样 ===================
    @Test
    @SuppressWarnings("unchecked")
    void t2_print_template_count_and_codes() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintTemplateRepository");
        Object repo = applicationContext.getBean(repoCls);
        Long cnt = (Long) repoCls.getMethod("count").invoke(repo);
        assertTrue(cnt >= 3, () -> "打印模板数应 >= 3,实际 " + cnt);

        List<Object> all = (List<Object>) repoCls.getMethod("findAll").invoke(repo);
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (Object row : all) {
            codes.add((String) row.getClass().getMethod("getCode").invoke(row));
        }
        assertTrue(codes.contains("CRM_CONTRACT"), "缺少 CRM_CONTRACT 模板,现有=" + codes);
        assertTrue(codes.contains("ERP_PURCHASE_ORDER"), "缺少 ERP_PURCHASE_ORDER 模板,现有=" + codes);
        assertTrue(codes.contains("WMS_SHIPMENT_NOTICE"), "缺少 WMS_SHIPMENT_NOTICE 模板,现有=" + codes);
    }

    // =================== (3) 3 模板渲染 contains 关键字段 (>=2 每个) ===================
    @Test
    @SuppressWarnings("unchecked")
    void t3_render_three_templates_contains_fields() throws Exception {
        Class<?> svcCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintRendererService");
        Object svc = applicationContext.getBean(svcCls);
        java.lang.reflect.Method renderC = svcCls.getMethod("renderContract", CrmContract.class);
        java.lang.reflect.Method renderP = svcCls.getMethod("renderPurchaseOrder", ErpPurchaseOrder.class);
        java.lang.reflect.Method renderS = svcCls.getMethod("renderShipmentNotice", WmsShipmentNotice.class);

        // --- (3a) CRM 合同 ---
        // 说明: 渲染器只是读 POJO getter, 不需要落库;省去 save 绕过 ORM 外键约束
        CrmCustomer cu = new CrmCustomer();
        cu.setName("打印测试客户");
        cu.setIndustryId(1);
        cu.setLevel(2);
        CrmContract cc = new CrmContract();
        cc.setNo("CRM-PRINT-001");
        cc.setName("年框服务合同");
        cc.setCustomer(cu);
        cc.setAmount(new BigDecimal("128000.00"));
        cc.setSignedDate(LocalDate.of(2026, 8, 1));
        cc.setStartDate(LocalDate.of(2026, 8, 1));
        cc.setEndDate(LocalDate.of(2027, 7, 31));
        cc.setStatus(ContractStatus.EFFECTIVE.code);

        String chtml = (String) renderC.invoke(svc, cc);
        assertNotNull(chtml);
        assertTrue(chtml.contains("CRM-PRINT-001"),
            () -> "CRM 合同打印 HTML 应 contains 合同号 CRM-PRINT-001,实际=" + snippet(chtml));
        assertTrue(chtml.contains("128000") || chtml.contains("128,000"),
            () -> "CRM 合同打印 HTML 应 contains 金额 128000,实际=" + snippet(chtml));

        // --- (3b) ERP 采购单 ---
        ErpSupplier sup = new ErpSupplier();
        sup.setCode("SUP-PRINT");
        sup.setName("打印测试供应商");
        sup.setContact("张打印");
        sup.setMobile("13800000000");
        ErpWarehouse erw = new ErpWarehouse();
        erw.setCode("ERP-WH-PR");
        erw.setName("ERP 打印测试仓库");
        ErpPurchaseOrder po = new ErpPurchaseOrder();
        po.setNo("ERP-PR-PO-001");
        po.setSupplier(sup);
        po.setWarehouse(erw);
        po.setOrderTime(LocalDateTime.of(2026, 8, 15, 10, 30));
        po.setTotalCount(new BigDecimal("25"));
        po.setTotalProductPrice(new BigDecimal("2500.00"));
        po.setTotalTaxPrice(new BigDecimal("325.00"));
        po.setTotalPrice(new BigDecimal("2825.00"));
        ErpPurchaseOrderItem i1 = new ErpPurchaseOrderItem();
        i1.setOrder(po);
        i1.setCount(new BigDecimal("10"));
        i1.setProductPrice(new BigDecimal("100.00"));
        i1.setTotalPrice(new BigDecimal("1000.00"));
        i1.setRemark("办公椅 ×10");
        ErpPurchaseOrderItem i2 = new ErpPurchaseOrderItem();
        i2.setOrder(po);
        i2.setCount(new BigDecimal("15"));
        i2.setProductPrice(new BigDecimal("100.00"));
        i2.setTotalPrice(new BigDecimal("1500.00"));
        i2.setRemark("办公桌 ×15");
        po.setItems(new ArrayList<>(List.of(i1, i2)));

        String phtml = (String) renderP.invoke(svc, po);
        assertNotNull(phtml);
        assertTrue(phtml.contains("ERP-PR-PO-001"),
            () -> "ERP 采购单打印应 contains 单号 ERP-PR-PO-001,实际=" + snippet(phtml));
        assertTrue(phtml.contains("25") || phtml.contains("25.00"),
            () -> "ERP 采购单打印应 contains 合计数量 25(totalCount),实际=" + snippet(phtml));
        assertTrue(phtml.contains("办公椅"),
            () -> "ERP 采购单明细行(remark)应 contains 商品名 办公椅,实际=" + snippet(phtml));

        // --- (3c) WMS 出库通知 ---
        WmsWarehouse wh = new WmsWarehouse();
        wh.setCode("WH-PR");
        wh.setName("打印测试仓库");
        wh.setAddress("测试地址 1 号");
        WmsZone zn = new WmsZone();
        zn.setCode("ZN-PR");
        zn.setName("打印区");
        zn.setWarehouse(wh);
        WmsShipmentNotice sn = new WmsShipmentNotice();
        sn.setNo("WMS-SN-PR-001");
        sn.setWarehouse(wh);
        sn.setCustomerName("CUSTOMER-PRINT-ABC");
        sn.setStatus(ShipmentNoticeStatus.NEW.code);
        sn.setRemark("打印测试出库单:共 120 件");

        String shtml = (String) renderS.invoke(svc, sn);
        assertNotNull(shtml);
        assertTrue(shtml.contains("WMS-SN-PR-001"),
            () -> "WMS 出库通知打印应 contains 单号 WMS-SN-PR-001,实际=" + snippet(shtml));
        assertTrue(shtml.contains("CUSTOMER-PRINT-ABC"),
            () -> "WMS 出库通知打印应 contains 客户名 CUSTOMER-PRINT-ABC,实际=" + snippet(shtml));
        assertTrue(shtml.contains("120"),
            () -> "WMS 出库通知打印应 contains 数量 120(备注里有),实际=" + snippet(shtml));
    }

    // =================== (4) TR-8.2 空数据一致行为:null 入参 → IAE (无 NPE) ===================
    @Test
    @SuppressWarnings("unchecked")
    void t4_null_entity_throws_iae_consistently() throws Exception {
        Class<?> svcCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintRendererService");
        Object svc = applicationContext.getBean(svcCls);
        java.lang.reflect.Method rc = svcCls.getMethod("renderContract", CrmContract.class);
        java.lang.reflect.Method rp = svcCls.getMethod("renderPurchaseOrder", ErpPurchaseOrder.class);
        java.lang.reflect.Method rs = svcCls.getMethod("renderShipmentNotice", WmsShipmentNotice.class);

        for (java.lang.reflect.Method m : List.of(rc, rp, rs)) {
            try {
                m.invoke(svc, (Object) null);
                fail(m.getName() + " 传 null entity 应抛 IllegalArgumentException");
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable c = ite.getCause();
                assertTrue(c instanceof IllegalArgumentException,
                    () -> m.getName() + " null 入参应抛 IAE,实际 cause="
                        + (c == null ? "null" : c.getClass().getSimpleName() + "(" + c.getMessage() + ")"));
            }
        }
    }

    private static String snippet(String s) {
        if (s == null) return "null";
        return s.length() < 500 ? s : s.substring(0, 480) + "...[truncated]";
    }
}
