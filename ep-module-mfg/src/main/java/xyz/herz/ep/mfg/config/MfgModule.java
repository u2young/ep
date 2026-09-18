package xyz.herz.ep.mfg.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.mfg.entity.bom.MfgBom;
import xyz.herz.ep.mfg.entity.jobcard.MfgJobCard;
import xyz.herz.ep.mfg.entity.operation.MfgOperation;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntry;
import xyz.herz.ep.mfg.entity.subcontract.MfgSubcontractingOrder;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.entity.workstation.MfgWorkstation;

/**
 * MFG 制造模块菜单注册
 */
@Component
public class MfgModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(MfgModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("mfg")
                .description("制造管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("mfg", "制造管理", "fa fa-cogs", 34);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("mfg-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/mfg.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(MfgOperation.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(MfgWorkstation.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(MfgBom.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(MfgWorkOrder.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(MfgJobCard.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(MfgStockEntry.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(MfgSubcontractingOrder.class, root, 7));
        return menus;
    }
}
