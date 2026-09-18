package xyz.herz.ep.pur.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.pur.entity.quotation.PurSupplierQuotation;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;
import xyz.herz.ep.pur.entity.requisition.PurPurchaseRequisition;
import xyz.herz.ep.pur.entity.rfq.PurRequestForQuotation;
import xyz.herz.ep.pur.entity.settings.PurPurchaseSettings;

/**
 * PUR 采购模块菜单注册
 */
@Component
public class PurModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(PurModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("pur")
                .description("采购管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("pur", "采购管理", "fa fa-shopping-cart", 37);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("pur-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/pur.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(PurPurchaseRequisition.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(PurRequestForQuotation.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(PurSupplierQuotation.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(PurPurchaseReceipt.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(PurPurchaseSettings.class, root, 5));
        return menus;
    }
}
