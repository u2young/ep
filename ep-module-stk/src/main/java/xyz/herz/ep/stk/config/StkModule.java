package xyz.herz.ep.stk.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.stk.entity.batch.StkBatch;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;
import xyz.herz.ep.stk.entity.reconciliation.StkStockReconciliation;
import xyz.herz.ep.stk.entity.serial.StkSerialNo;
import xyz.herz.ep.stk.entity.settings.StkStockSettings;

/**
 * STK 库存模块菜单注册
 */
@Component
public class StkModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(StkModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("stk")
                .description("库存管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("stk", "库存管理", "fa fa-archive", 40);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("stk-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/stk.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(StkBatch.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(StkSerialNo.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(StkStockEntry.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(StkStockReconciliation.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(StkStockSettings.class, root, 5));
        return menus;
    }
}
