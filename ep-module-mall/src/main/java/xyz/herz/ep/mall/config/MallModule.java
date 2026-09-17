package xyz.herz.ep.mall.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.mall.entity.MallPayOrder;
import xyz.herz.ep.mall.entity.MallProductBrand;
import xyz.herz.ep.mall.entity.MallProductCategory;
import xyz.herz.ep.mall.entity.MallProductSku;
import xyz.herz.ep.mall.entity.MallProductSpu;
import xyz.herz.ep.mall.entity.MallTradeAfterSale;
import xyz.herz.ep.mall.entity.MallTradeAfterSaleLog;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;

/**
 * 商城模块菜单注册
 */
@Component
public class MallModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(MallModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("mall")
                .description("商城管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("mall", "商城管理", "fa fa-shopping-cart", 60);
        menus.add(root);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("mall-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/mall.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(MallProductSpu.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(MallProductSku.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(MallProductCategory.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(MallProductBrand.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(MallTradeOrder.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(MallTradeOrderItem.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(MallTradeAfterSale.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(MallTradeAfterSaleLog.class, root, 8));
        menus.add(MetaMenu.createEruptClassMenu(MallPayOrder.class, root, 9));

        return menus;
    }
}
