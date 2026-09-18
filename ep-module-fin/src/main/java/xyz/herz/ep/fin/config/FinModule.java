package xyz.herz.ep.fin.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.budget.FinBudget;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.entity.payment.FinPaymentEntry;

/**
 * FIN 财务模块菜单注册
 */
@Component
public class FinModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(FinModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("fin")
                .description("财务管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("fin", "财务管理", "fa fa-money", 32);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("fin-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/fin.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(FinAccount.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(FinCostCenter.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(FinJournalEntry.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(FinBudget.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(FinSalesInvoice.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(FinPurchaseInvoice.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(FinPaymentEntry.class, root, 7));
        return menus;
    }
}
