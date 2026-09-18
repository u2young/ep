package xyz.herz.ep.sal.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.sal.entity.deliverynote.SalDeliveryNote;
import xyz.herz.ep.sal.entity.quotation.SalQuotation;
import xyz.herz.ep.sal.entity.salesorder.SalSalesOrder;
import xyz.herz.ep.sal.entity.salespartner.SalSalesPartner;
import xyz.herz.ep.sal.entity.salesperson.SalSalesPerson;

/**
 * SAL 销售模块菜单注册
 */
@Component
public class SalModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(SalModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("sal")
                .description("销售管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("sal", "销售管理", "fa fa-line-chart", 41);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("sal-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/sal.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(SalSalesPerson.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(SalSalesPartner.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(SalQuotation.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(SalSalesOrder.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(SalDeliveryNote.class, root, 5));
        return menus;
    }
}
