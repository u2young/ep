package xyz.herz.ep.wms.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.wms.entity.WmsAsn;
import xyz.herz.ep.wms.entity.WmsAsnItem;
import xyz.herz.ep.wms.entity.WmsLocation;
import xyz.herz.ep.wms.entity.WmsPick;
import xyz.herz.ep.wms.entity.WmsPickItem;
import xyz.herz.ep.wms.entity.WmsPutaway;
import xyz.herz.ep.wms.entity.WmsPutawayItem;
import xyz.herz.ep.wms.entity.WmsReceipt;
import xyz.herz.ep.wms.entity.WmsReceiptItem;
import xyz.herz.ep.wms.entity.WmsShipmentItem;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;
import xyz.herz.ep.wms.entity.WmsStock;
import xyz.herz.ep.wms.entity.WmsStockCheck;
import xyz.herz.ep.wms.entity.WmsStockCheckItem;
import xyz.herz.ep.wms.entity.WmsStockMove;
import xyz.herz.ep.wms.entity.WmsStockMoveOrder;
import xyz.herz.ep.wms.entity.WmsStockMoveOrderItem;
import xyz.herz.ep.wms.entity.WmsWarehouse;
import xyz.herz.ep.wms.entity.WmsZone;

/**
 * WMS仓储模块菜单注册
 */
@Component
public class WmsModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(WmsModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("wms")
                .description("仓储管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("wms", "WMS仓储管理", "fa fa-truck", 40);
        menus.add(root);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("wms-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/wms.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(WmsWarehouse.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(WmsZone.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(WmsLocation.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(WmsStock.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(WmsStockCheck.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(WmsStockCheckItem.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(WmsAsn.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(WmsAsnItem.class, root, 8));
        menus.add(MetaMenu.createEruptClassMenu(WmsReceipt.class, root, 9));
        menus.add(MetaMenu.createEruptClassMenu(WmsReceiptItem.class, root, 10));
        menus.add(MetaMenu.createEruptClassMenu(WmsPutaway.class, root, 11));
        menus.add(MetaMenu.createEruptClassMenu(WmsPutawayItem.class, root, 12));
        menus.add(MetaMenu.createEruptClassMenu(WmsPick.class, root, 13));
        menus.add(MetaMenu.createEruptClassMenu(WmsPickItem.class, root, 14));
        menus.add(MetaMenu.createEruptClassMenu(WmsShipmentNotice.class, root, 15));
        menus.add(MetaMenu.createEruptClassMenu(WmsShipmentItem.class, root, 16));
        menus.add(MetaMenu.createEruptClassMenu(WmsStockMove.class, root, 17));
        menus.add(MetaMenu.createEruptClassMenu(WmsStockMoveOrder.class, root, 18));
        menus.add(MetaMenu.createEruptClassMenu(WmsStockMoveOrderItem.class, root, 19));

        return menus;
    }
}
