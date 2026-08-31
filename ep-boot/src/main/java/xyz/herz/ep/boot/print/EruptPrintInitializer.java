package xyz.herz.ep.boot.print;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * erupt-print 启动初始化器: 当 ep_print_template 为空时插入 3 张模板。
 * 覆盖 AC-5: CRM 合同 / ERP 采购单 / WMS 出库通知。
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
