package xyz.herz.ep.boot.print;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * erupt-print 启动初始化器: 当 ep_print_template 为空时插入 14 张模板。
 * 覆盖 AC-5: CRM 合同 / ERP 采购单 / WMS 出库通知 + Fin 销售发票 / Fin 采购发票 + Mfg 工单 + Proj 验收单 + Sup 工单 Ticket + Ast 资产卡片 + HR 员工档案 + Pay 工资单 + Qal 质检报告 + Pur 采购收货单 + Stk 库存出入库单。
 */
@Component
public class EruptPrintInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final EruptPrintTemplateRepository repo;

    public EruptPrintInitializer(EruptPrintTemplateRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repo.count() > 0) return;
        List<EruptPrintTemplate> list = new ArrayList<>();
        list.add(build("CRM_CONTRACT", "CRM 合同打印单", "CRM", "crm-contract.html"));
        list.add(build("ERP_PURCHASE_ORDER", "ERP 采购单打印", "ERP", "erp-purchase-order.html"));
        list.add(build("WMS_SHIPMENT_NOTICE", "WMS 出库通知单打印", "WMS", "wms-shipment-notice.html"));
        list.add(build("FIN_SALES_INVOICE", "财务销售发票打印", "Fin", "fin-sales-invoice.html"));
        list.add(build("FIN_PURCHASE_INVOICE", "财务采购发票打印", "Fin", "fin-purchase-invoice.html"));
        list.add(build("MFG_WORK_ORDER", "制造工单打印", "Mfg", "mfg-work-order.html"));
        list.add(build("PROJ_ACCEPTANCE", "项目验收单打印", "Proj", "proj-acceptance.html"));
        list.add(build("SUP_TICKET", "客户工单 Ticket 打印", "Sup", "sup-ticket.html"));
        list.add(build("AST_ASSET_CARD", "固定资产卡片打印", "Ast", "ast-asset-card.html"));
        list.add(build("HR_EMPLOYEE_PROFILE", "员工档案打印", "HR", "hr-employee-profile.html"));
        list.add(build("PAY_SALARY_SLIP", "工资单打印", "Pay", "pay-salary-slip.html"));
        list.add(build("QAL_INSPECTION_REPORT", "质检报告打印", "Qal", "qal-inspection-report.html"));
        list.add(build("PUR_PURCHASE_ORDER", "采购收货单打印", "Pur", "pur-purchase-receipt.html"));
        list.add(build("STK_STOCK_ENTRY", "库存出入库单打印", "Stk", "stk-stock-entry.html"));
        repo.saveAll(list);
    }

    private static EruptPrintTemplate build(String code, String name, String module, String tplKey) {
        EruptPrintTemplate t = new EruptPrintTemplate();
        t.setCode(code);
        t.setName(name);
        t.setBizModule(module);
        t.setTplKey(tplKey);
        t.setRemark("自动初始化 " + module + " 打印模板 code=" + code);
        return t;
    }
}
