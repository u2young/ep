package xyz.herz.ep.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;
import xyz.herz.ep.pay.entity.slip.PaySalarySlipItem;
import xyz.herz.ep.qal.entity.inspection.QalInspection;
import xyz.herz.ep.qal.entity.inspection.QalInspectionItem;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;
import xyz.herz.ep.pur.entity.receipt.PurReceiptItem;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;
import xyz.herz.ep.stk.entity.entry.StkStockEntryItem;
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

import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sal.entity.quotation.SalQuotation;
import xyz.herz.ep.sal.entity.quotation.SalQuotationItem;
import xyz.herz.ep.boot.print.EruptPrintRendererService;

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
    @Autowired EruptPrintRendererService printRenderer;

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

    // =================== (2) 模板数 >=8 + code 抽样 ===================
    @Test
    @SuppressWarnings("unchecked")
    void t2_print_template_count_and_codes() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintTemplateRepository");
        Object repo = applicationContext.getBean(repoCls);
        Long cnt = (Long) repoCls.getMethod("count").invoke(repo);
        assertTrue(cnt >= 14, () -> "打印模板数应 >= 14,实际 " + cnt
            + " (14 模板: CRM/ERP/WMS/Fin×2/Mfg/Proj/Sup/Ast/HR/Pay/Qal/Pur/Stk)");

        List<Object> all = (List<Object>) repoCls.getMethod("findAll").invoke(repo);
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (Object row : all) {
            codes.add((String) row.getClass().getMethod("getCode").invoke(row));
        }
        assertTrue(codes.contains("CRM_CONTRACT"), "缺少 CRM_CONTRACT 模板,现有=" + codes);
        assertTrue(codes.contains("ERP_PURCHASE_ORDER"), "缺少 ERP_PURCHASE_ORDER 模板,现有=" + codes);
        assertTrue(codes.contains("WMS_SHIPMENT_NOTICE"), "缺少 WMS_SHIPMENT_NOTICE 模板,现有=" + codes);
        assertTrue(codes.contains("FIN_SALES_INVOICE"), "缺少 FIN_SALES_INVOICE 模板,现有=" + codes);
        assertTrue(codes.contains("FIN_PURCHASE_INVOICE"), "缺少 FIN_PURCHASE_INVOICE 模板,现有=" + codes);
        assertTrue(codes.contains("MFG_WORK_ORDER"), "缺少 MFG_WORK_ORDER 模板,现有=" + codes);
        assertTrue(codes.contains("PROJ_ACCEPTANCE"), "缺少 PROJ_ACCEPTANCE 模板,现有=" + codes);
        assertTrue(codes.contains("SUP_TICKET"), "缺少 SUP_TICKET 模板,现有=" + codes);
        assertTrue(codes.contains("AST_ASSET_CARD"), "缺少 AST_ASSET_CARD 模板,现有=" + codes);
        assertTrue(codes.contains("HR_EMPLOYEE_PROFILE"), "缺少 HR_EMPLOYEE_PROFILE 模板,现有=" + codes);
        assertTrue(codes.contains("PAY_SALARY_SLIP"), "缺少 PAY_SALARY_SLIP 模板,现有=" + codes);
        assertTrue(codes.contains("QAL_INSPECTION_REPORT"), "缺少 QAL_INSPECTION_REPORT 模板,现有=" + codes);
        assertTrue(codes.contains("PUR_PURCHASE_ORDER"), "缺少 PUR_PURCHASE_ORDER 模板,现有=" + codes);
        assertTrue(codes.contains("STK_STOCK_ENTRY"), "缺少 STK_STOCK_ENTRY 模板,现有=" + codes);
        assertTrue(codes.contains("SAL_QUOTATION"), "缺少 SAL_QUOTATION 模板,现有=" + codes);
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
        java.lang.reflect.Method rsi = svcCls.getMethod("renderSalesInvoice", FinSalesInvoice.class);
        java.lang.reflect.Method rpi = svcCls.getMethod("renderPurchaseInvoice", FinPurchaseInvoice.class);
        java.lang.reflect.Method rmfg = svcCls.getMethod("renderWorkOrder",
            Class.forName("xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder"));
        java.lang.reflect.Method rproj = svcCls.getMethod("renderProject",
            Class.forName("xyz.herz.ep.proj.entity.project.ProjProject"));
        java.lang.reflect.Method rsup = svcCls.getMethod("renderTicket",
            Class.forName("xyz.herz.ep.sup.entity.issue.SupIssue"));
        java.lang.reflect.Method rast = svcCls.getMethod("renderAssetCard",
            Class.forName("xyz.herz.ep.ast.entity.asset.AstAsset"));
        java.lang.reflect.Method rhr = svcCls.getMethod("renderEmployeeProfile",
            Class.forName("xyz.herz.ep.hr.entity.employee.HrEmployee"));
        java.lang.reflect.Method rpay = svcCls.getMethod("renderSalarySlip",
            Class.forName("xyz.herz.ep.pay.entity.slip.PaySalarySlip"));
        java.lang.reflect.Method rqal = svcCls.getMethod("renderInspectionReport",
            Class.forName("xyz.herz.ep.qal.entity.inspection.QalInspection"));
        java.lang.reflect.Method rpur = svcCls.getMethod("renderPurchaseReceipt",
            Class.forName("xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt"));
        java.lang.reflect.Method rstk = svcCls.getMethod("renderStockEntry",
            Class.forName("xyz.herz.ep.stk.entity.entry.StkStockEntry"));

        for (java.lang.reflect.Method m : List.of(rc, rp, rs, rsi, rpi, rmfg, rproj, rsup, rast, rhr, rpay, rqal, rpur, rstk)) {
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

    // =================== (5) Fin 销售/采购发票渲染 contains 关键字段 (>=2 每个) ===================
    @Test
    void t5_render_fin_invoice_templates_contains_fields() {
        // --- 销售发票 ---
        FinSalesInvoice si = new FinSalesInvoice();
        si.setNo("FIN-SI-PRINT-001");
        si.setCustomerName("财务打印测试客户");
        si.setPostingDate(LocalDate.of(2026, 8, 31));
        si.setDueDate(LocalDate.of(2026, 9, 30));
        si.setTotal(new BigDecimal("12800.00"));
        si.setGrandTotal(new BigDecimal("12800.00"));
        si.setPaidAmount(new BigDecimal("5000.00"));
        si.setOutstandingAmount(new BigDecimal("7800.00"));
        si.setRemark("销售发票打印测试");

        String siHtml = printRenderer.renderSalesInvoice(si);
        assertNotNull(siHtml);
        assertTrue(siHtml.contains("FIN-SI-PRINT-001"),
            () -> "销售发票打印应 contains 发票号 FIN-SI-PRINT-001,实际=" + snippet(siHtml));
        assertTrue(siHtml.contains("财务打印测试客户"),
            () -> "销售发票打印应 contains 客户名,实际=" + snippet(siHtml));
        assertTrue(siHtml.contains("7800") || siHtml.contains("7,800"),
            () -> "销售发票打印应 contains 未付金额 7800,实际=" + snippet(siHtml));

        // --- 采购发票 ---
        FinPurchaseInvoice pi = new FinPurchaseInvoice();
        pi.setNo("FIN-PI-PRINT-002");
        pi.setSupplierName("财务打印测试供应商");
        pi.setPostingDate(LocalDate.of(2026, 8, 31));
        pi.setDueDate(LocalDate.of(2026, 9, 30));
        pi.setTotal(new BigDecimal("9600.00"));
        pi.setGrandTotal(new BigDecimal("9600.00"));
        pi.setPaidAmount(BigDecimal.ZERO);
        pi.setOutstandingAmount(new BigDecimal("9600.00"));
        pi.setRemark("采购发票打印测试");

        String piHtml = printRenderer.renderPurchaseInvoice(pi);
        assertNotNull(piHtml);
        assertTrue(piHtml.contains("FIN-PI-PRINT-002"),
            () -> "采购发票打印应 contains 发票号 FIN-PI-PRINT-002,实际=" + snippet(piHtml));
        assertTrue(piHtml.contains("财务打印测试供应商"),
            () -> "采购发票打印应 contains 供应商名,实际=" + snippet(piHtml));
        assertTrue(piHtml.contains("9600") || piHtml.contains("9,600"),
            () -> "采购发票打印应 contains 未付金额 9600,实际=" + snippet(piHtml));
    }

    // =================== (6) Mfg 工单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t6_render_mfg_workorder_contains_fields() throws Exception {
        Class<?> woCls = Class.forName("xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder");
        Object wo = woCls.getDeclaredConstructor().newInstance();

        // 产品(只需 name getter,用 ErpProduct POJO 即可,无需落库)
        Class<?> prodCls = Class.forName("xyz.herz.ep.erp.entity.product.ErpProduct");
        Object prod = prodCls.getDeclaredConstructor().newInstance();
        prodCls.getMethod("setName", String.class).invoke(prod, "打印测试产品-A");

        // 领料仓 / 入库仓(ErpWarehouse POJO)
        Class<?> whCls = Class.forName("xyz.herz.ep.erp.entity.master.ErpWarehouse");
        Object srcWh = whCls.getDeclaredConstructor().newInstance();
        whCls.getMethod("setName", String.class).invoke(srcWh, "原料仓-PR");
        Object tgtWh = whCls.getDeclaredConstructor().newInstance();
        whCls.getMethod("setName", String.class).invoke(tgtWh, "成品仓-PR");

        woCls.getMethod("setNo", String.class).invoke(wo, "MFG-WO-PR-001");
        woCls.getMethod("setProduct", prodCls).invoke(wo, prod);
        woCls.getMethod("setQty", java.math.BigDecimal.class)
            .invoke(wo, new java.math.BigDecimal("100"));
        woCls.getMethod("setProducedQty", java.math.BigDecimal.class)
            .invoke(wo, new java.math.BigDecimal("40"));
        woCls.getMethod("setStatus", Integer.class).invoke(wo, 2);
        woCls.getMethod("setRemark", String.class).invoke(wo, "工单打印测试备注");

        Class<?> svcCls = Class.forName("xyz.herz.ep.boot.print.EruptPrintRendererService");
        Object svc = applicationContext.getBean(svcCls);
        java.lang.reflect.Method renderWo = svcCls.getMethod("renderWorkOrder", woCls);
        String html = (String) renderWo.invoke(svc, wo);

        assertNotNull(html);
        assertTrue(html.contains("MFG-WO-PR-001"),
            () -> "制造工单打印应 contains 工单号 MFG-WO-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试产品-A"),
            () -> "制造工单打印应 contains 产品名 打印测试产品-A,实际=" + snippet(html));
        assertTrue(html.contains("100"),
            () -> "制造工单打印应 contains 计划数量 100,实际=" + snippet(html));
        assertTrue(html.contains("MFG_WORK_ORDER"),
            () -> "制造工单打印应 contains print code MFG_WORK_ORDER,实际=" + snippet(html));
    }

    // =================== (7) Sup 客户工单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t7_render_sup_ticket_contains_fields() {
        CrmCustomer cu = new CrmCustomer();
        cu.setName("打印测试客户-SUP");
        SupIssue iss = new SupIssue();
        iss.setSubject("SUP-PRINT-001 工单主题测试");
        iss.setCustomer(cu);
        iss.setPriority(2);
        iss.setStatus(1);
        iss.setRaisedBy("张三");
        iss.setAssigneeName("客服李四");
        iss.setSlaFulfilled(Boolean.TRUE);
        iss.setDescription("工单描述:系统登录异常");
        iss.setRemark("工单打印测试备注");

        String html = printRenderer.renderTicket(iss);
        assertNotNull(html);
        assertTrue(html.contains("SUP-PRINT-001"),
            () -> "客户工单打印应 contains 工单主题 SUP-PRINT-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试客户-SUP"),
            () -> "客户工单打印应 contains 客户名 打印测试客户-SUP,实际=" + snippet(html));
        assertTrue(html.contains("客服李四"),
            () -> "客户工单打印应 contains 分派给 客服李四,实际=" + snippet(html));
        assertTrue(html.contains("SUP_TICKET"),
            () -> "客户工单打印应 contains print code SUP_TICKET,实际=" + snippet(html));
    }

    // =================== (8) Ast 资产卡片渲染 contains 关键字段 (>=2) ===================
    @Test
    void t8_render_ast_asset_card_contains_fields() {
        AstAsset asset = new AstAsset();
        asset.setAssetNo("AST-PR-001");
        asset.setName("打印测试笔记本-资产");
        asset.setStatus(1);
        asset.setPurchaseDate(LocalDate.of(2026, 1, 15));
        asset.setPurchaseAmount(new BigDecimal("8888.00"));
        asset.setSalvageValue(new BigDecimal("500.00"));
        asset.setTotalDepreciation(new BigDecimal("1200.00"));
        asset.setCurrentValue(new BigDecimal("7688.00"));
        asset.setDepreciationMethod(1);
        asset.setUsefulLifeMonths(36);
        asset.setDepreciatedPeriods(6);
        asset.setCustodianName("资产保管人-王打印");
        asset.setCostCenterId(7701L);
        asset.setRemark("资产卡片打印测试备注");

        String html = printRenderer.renderAssetCard(asset);
        assertNotNull(html);
        assertTrue(html.contains("AST-PR-001"),
            () -> "资产卡片打印应 contains 资产编号 AST-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试笔记本-资产"),
            () -> "资产卡片打印应 contains 资产名称,实际=" + snippet(html));
        assertTrue(html.contains("资产保管人-王打印"),
            () -> "资产卡片打印应 contains 保管人 资产保管人-王打印,实际=" + snippet(html));
        assertTrue(html.contains("AST_ASSET_CARD"),
            () -> "资产卡片打印应 contains print code AST_ASSET_CARD,实际=" + snippet(html));
        assertTrue(html.contains("8888") || html.contains("8,888"),
            () -> "资产卡片打印应 contains 原值 8888,实际=" + snippet(html));
    }

    // =================== (9) HR 员工档案渲染 contains 关键字段 (>=2) ===================
    @Test
    void t9_render_hr_employee_profile_contains_fields() {
        HrEmployee emp = new HrEmployee();
        emp.setEmpNo("HR-PR-001");
        emp.setName("打印测试员工-李档案");
        emp.setStatus(1);
        emp.setGender(0);
        emp.setBirthDate(LocalDate.of(1995, 6, 1));
        emp.setHireDate(LocalDate.of(2026, 3, 1));
        emp.setPhone("13900000001");
        emp.setEmail("print-hr@test.local");
        emp.setIdType(0);
        emp.setIdNo("110101199506011234");
        emp.setAddress("北京市海淀区打印路 1 号");
        emp.setRemark("员工档案打印测试备注");

        String html = printRenderer.renderEmployeeProfile(emp);
        assertNotNull(html);
        assertTrue(html.contains("HR-PR-001"),
            () -> "员工档案打印应 contains 工号 HR-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试员工-李档案"),
            () -> "员工档案打印应 contains 姓名,实际=" + snippet(html));
        assertTrue(html.contains("13900000001"),
            () -> "员工档案打印应 contains 手机号,实际=" + snippet(html));
        assertTrue(html.contains("HR_EMPLOYEE_PROFILE"),
            () -> "员工档案打印应 contains print code HR_EMPLOYEE_PROFILE,实际=" + snippet(html));
        assertTrue(html.contains("110101199506011234"),
            () -> "员工档案打印应 contains 证件号码,实际=" + snippet(html));
    }

    // =================== (10) Pay 工资单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t10_render_pay_salary_slip_contains_fields() {
        PaySalarySlip slip = new PaySalarySlip();
        slip.setSlipNo("PAY-SLIP-PR-001");
        slip.setPostingMonth("2026-08");
        slip.setStatus(2);
        slip.setGrossPay(new BigDecimal("12000.00"));
        slip.setDeductions(new BigDecimal("2600.00"));
        slip.setNetPay(new BigDecimal("9400.00"));
        slip.setRemark("工资单打印测试备注");

        PaySalarySlipItem it1 = new PaySalarySlipItem();
        it1.setSlip(slip);
        it1.setComponentCode("BASIC");
        it1.setComponentName("基本工资");
        it1.setComponentType(1);
        it1.setAmount(new BigDecimal("10000.00"));
        PaySalarySlipItem it2 = new PaySalarySlipItem();
        it2.setSlip(slip);
        it2.setComponentCode("BONUS");
        it2.setComponentName("绩效奖金");
        it2.setComponentType(1);
        it2.setAmount(new BigDecimal("2000.00"));
        slip.setItems(new ArrayList<>(List.of(it1, it2)));

        String html = printRenderer.renderSalarySlip(slip);
        assertNotNull(html);
        assertTrue(html.contains("PAY-SLIP-PR-001"),
            () -> "工资单打印应 contains 工资单号 PAY-SLIP-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("2026-08"),
            () -> "工资单打印应 contains 薪酬月份 2026-08,实际=" + snippet(html));
        assertTrue(html.contains("基本工资"),
            () -> "工资单打印应 contains 明细组件 基本工资,实际=" + snippet(html));
        assertTrue(html.contains("9400") || html.contains("9,400"),
            () -> "工资单打印应 contains 实发合计 9400,实际=" + snippet(html));
        assertTrue(html.contains("PAY_SALARY_SLIP"),
            () -> "工资单打印应 contains print code PAY_SALARY_SLIP,实际=" + snippet(html));
    }

    // =================== (11) Qal 质检报告渲染 contains 关键字段 (>=2) ===================
    @Test
    void t11_render_qal_inspection_report_contains_fields() {
        QalInspection insp = new QalInspection();
        insp.setInspectionNo("QAL-PR-001");
        insp.setSourceType("PurchaseReceipt");
        insp.setSourceNo("PR-PRINT-001");
        insp.setItemCode("ITEM-PR-001");
        insp.setItemName("打印测试轴套");
        insp.setBatchNo("B-PR-001");
        insp.setQty(new BigDecimal("100"));
        insp.setSampleQty(new BigDecimal("5"));
        insp.setStatus(2);
        insp.setInspector("质检员-王打印");
        insp.setRemark("质检报告打印测试备注");

        QalInspectionItem it1 = new QalInspectionItem();
        it1.setInspection(insp);
        it1.setCriteriaName("外径尺寸");
        it1.setSpecText("[9.5, 10.5] mm");
        it1.setReadingValue(new BigDecimal("10.0"));
        it1.setIsPass(true);
        QalInspectionItem it2 = new QalInspectionItem();
        it2.setInspection(insp);
        it2.setCriteriaName("外观");
        it2.setSpecText("目测");
        it2.setIsPass(false);
        insp.setItems(new ArrayList<>(List.of(it1, it2)));

        String html = printRenderer.renderInspectionReport(insp);
        assertNotNull(html);
        assertTrue(html.contains("QAL-PR-001"),
            () -> "质检报告打印应 contains 质检单号 QAL-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试轴套"),
            () -> "质检报告打印应 contains 物料名称,实际=" + snippet(html));
        assertTrue(html.contains("质检员-王打印"),
            () -> "质检报告打印应 contains 检查员,实际=" + snippet(html));
        assertTrue(html.contains("外径尺寸"),
            () -> "质检报告打印应 contains 检验参数 外径尺寸,实际=" + snippet(html));
        assertTrue(html.contains("QAL_INSPECTION_REPORT"),
            () -> "质检报告打印应 contains print code QAL_INSPECTION_REPORT,实际=" + snippet(html));
    }

    // =================== (12) Pur 采购收货单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t12_render_pur_purchase_receipt_contains_fields() {
        PurPurchaseReceipt rcp = new PurPurchaseReceipt();
        rcp.setReceiptNo("PUR-PR-001");
        rcp.setSupplierCode("SUP-PR-001");
        rcp.setSupplierName("打印测试供应商-采购");
        rcp.setSourceQuotationNo("SQ-PR-001");
        rcp.setReceiptDate(LocalDate.of(2026, 8, 31));
        rcp.setReceivedQty(new BigDecimal("15"));
        rcp.setReceivedAmount(new BigDecimal("2000.00"));
        rcp.setReceiver("收货员-赵打印");
        rcp.setStatus(2);
        rcp.setRemark("采购收货单打印测试备注");

        PurReceiptItem i1 = new PurReceiptItem();
        i1.setReceipt(rcp);
        i1.setItemCode("ITEM-PR-001");
        i1.setItemName("打印测试物料A");
        i1.setOrderedQty(new BigDecimal("10"));
        i1.setReceivedQty(new BigDecimal("10"));
        i1.setUnitPrice(new BigDecimal("100.00"));
        i1.setAmount(new BigDecimal("1000.00"));
        PurReceiptItem i2 = new PurReceiptItem();
        i2.setReceipt(rcp);
        i2.setItemCode("ITEM-PR-002");
        i2.setItemName("打印测试物料B");
        i2.setOrderedQty(new BigDecimal("5"));
        i2.setReceivedQty(new BigDecimal("5"));
        i2.setUnitPrice(new BigDecimal("200.00"));
        i2.setAmount(new BigDecimal("1000.00"));
        rcp.setItems(new ArrayList<>(List.of(i1, i2)));

        String html = printRenderer.renderPurchaseReceipt(rcp);
        assertNotNull(html);
        assertTrue(html.contains("PUR-PR-001"),
            () -> "采购收货单打印应 contains 收货单号 PUR-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试供应商-采购"),
            () -> "采购收货单打印应 contains 供应商名,实际=" + snippet(html));
        assertTrue(html.contains("打印测试物料A"),
            () -> "采购收货单打印应 contains 物料名称 打印测试物料A,实际=" + snippet(html));
        assertTrue(html.contains("收货员-赵打印"),
            () -> "采购收货单打印应 contains 收货人 收货员-赵打印,实际=" + snippet(html));
        assertTrue(html.contains("PUR_PURCHASE_ORDER"),
            () -> "采购收货单打印应 contains print code PUR_PURCHASE_ORDER,实际=" + snippet(html));
        assertTrue(html.contains("2000") || html.contains("2,000"),
            () -> "采购收货单打印应 contains 收货金额合计 2000,实际=" + snippet(html));
    }

    // =================== (13) Stk 库存出入库单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t13_render_stk_stock_entry_contains_fields() {
        StkStockEntry entry = new StkStockEntry();
        entry.setEntryNo("STK-PR-001");
        entry.setEntryType(1); // 入库
        entry.setSourceWarehouseCode(null);
        entry.setTargetWarehouseCode("WH-TGT-PR");
        entry.setTargetWarehouseName("打印测试目标仓");
        entry.setPostingDate(LocalDate.of(2026, 8, 31));
        entry.setTotalQty(new BigDecimal("15"));
        entry.setTotalAmount(new BigDecimal("2000.00"));
        entry.setOperator("库管员-孙打印");
        entry.setStatus(1); // 已提交
        entry.setRemark("库存出入库单打印测试备注");

        StkStockEntryItem i1 = new StkStockEntryItem();
        i1.setEntry(entry);
        i1.setItemCode("ITEM-PR-001");
        i1.setItemName("打印测试物料A");
        i1.setQty(new BigDecimal("10"));
        i1.setUnitPrice(new BigDecimal("100.00"));
        i1.setAmount(new BigDecimal("1000.00"));
        i1.setBatchNo("BATCH-PR-001");
        StkStockEntryItem i2 = new StkStockEntryItem();
        i2.setEntry(entry);
        i2.setItemCode("ITEM-PR-002");
        i2.setItemName("打印测试物料B");
        i2.setQty(new BigDecimal("5"));
        i2.setUnitPrice(new BigDecimal("200.00"));
        i2.setAmount(new BigDecimal("1000.00"));
        i2.setSerialNo("SN-PR-001");
        entry.setItems(new ArrayList<>(List.of(i1, i2)));

        String html = printRenderer.renderStockEntry(entry);
        assertNotNull(html);
        assertTrue(html.contains("STK-PR-001"),
            () -> "库存出入库单打印应 contains 单号 STK-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("WH-TGT-PR"),
            () -> "库存出入库单打印应 contains 目标仓编码 WH-TGT-PR,实际=" + snippet(html));
        assertTrue(html.contains("打印测试物料A"),
            () -> "库存出入库单打印应 contains 物料名称 打印测试物料A,实际=" + snippet(html));
        assertTrue(html.contains("库管员-孙打印"),
            () -> "库存出入库单打印应 contains 操作人 库管员-孙打印,实际=" + snippet(html));
        assertTrue(html.contains("BATCH-PR-001"),
            () -> "库存出入库单打印应 contains 批次号 BATCH-PR-001,实际=" + snippet(html));
        assertTrue(html.contains("STK_STOCK_ENTRY"),
            () -> "库存出入库单打印应 contains print code STK_STOCK_ENTRY,实际=" + snippet(html));
    }

    // =================== (14) Sal 报价单渲染 contains 关键字段 (>=2) ===================
    @Test
    void t14_render_sal_quotation_contains_fields() {
        SalQuotation q = new SalQuotation();
        q.setQuotationNo("SAL-QTN-001");
        q.setCustomerCode("CUST-PR-01");
        q.setCustomerName("打印测试客户");
        q.setOrderType(1);
        q.setExpectedAmount(new BigDecimal("5000.00"));
        q.setValidDate(LocalDate.of(2026, 12, 31));
        q.setCreatedBy("打印测试人");
        q.setStatus(0);
        q.setRemark("打印测试备注");

        SalQuotationItem i1 = new SalQuotationItem();
        i1.setQuotation(q);
        i1.setItemCode("ITEM-PR-01");
        i1.setItemName("打印测试物料");
        i1.setQty(new BigDecimal("10"));
        i1.setUnitPrice(new BigDecimal("500.00"));
        i1.setAmount(new BigDecimal("5000.00"));
        q.setItems(List.of(i1));

        String html = printRenderer.renderQuotation(q);
        assertNotNull(html);
        assertTrue(html.contains("SAL-QTN-001"),
            () -> "报价单打印应 contains 单号 SAL-QTN-001,实际=" + snippet(html));
        assertTrue(html.contains("打印测试客户"),
            () -> "报价单打印应 contains 客户名 打印测试客户,实际=" + snippet(html));
        assertTrue(html.contains("打印测试物料"),
            () -> "报价单打印应 contains 物料名 打印测试物料,实际=" + snippet(html));
        assertTrue(html.contains("SAL_QUOTATION"),
            () -> "报价单打印应 contains print code SAL_QUOTATION,实际=" + snippet(html));
    }

    private static String snippet(String s) {
        if (s == null) return "null";
        return s.length() < 500 ? s : s.substring(0, 480) + "...[truncated]";
    }
}
