package xyz.herz.ep.pay.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.pay.entity.component.PaySalaryComponent;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructure;

/**
 * PAY 薪资模块菜单注册
 */
@Component
public class PayModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(PayModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("pay")
                .description("薪资管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("pay", "薪资管理", "fa fa-paypal", 35);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("pay-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/pay.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(PaySalaryComponent.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(PaySalaryStructure.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(PaySalarySlip.class, root, 3));
        return menus;
    }
}
