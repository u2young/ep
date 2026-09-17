package xyz.herz.ep.landing.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.landing.entity.LandingAccessLog;
import xyz.herz.ep.landing.entity.LandingCoupon;
import xyz.herz.ep.landing.entity.LandingLead;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.entity.LandingSeckill;
import xyz.herz.ep.landing.entity.LandingSeckillOrder;
import xyz.herz.ep.landing.entity.LandingTemplate;
import xyz.herz.ep.landing.entity.LandingUserCoupon;

/**
 * 落地页模块菜单注册
 */
@Component
public class LandingModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(LandingModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("landing")
                .description("落地页管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("landing", "落地页管理", "fa fa-rocket", 10);
        menus.add(root);

        // 页面设计器:amis 可视化编辑器,新窗口打开(无 ?id= 时先选页面)
        MetaMenu designer = new MetaMenu();
        designer.setCode("landing-designer");
        designer.setName("页面设计器");
        designer.setIcon("fa fa-pencil");
        designer.setValue("/landing-editor.html");
        designer.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        designer.setStatus(MenuStatus.OPEN);
        designer.setParentMenu(root);
        designer.setSort(0);
        menus.add(designer);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("landing-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/landing.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(1);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(LandingPage.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(LandingTemplate.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(LandingSeckill.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(LandingCoupon.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(LandingLead.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(LandingSeckillOrder.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(LandingUserCoupon.class, root, 8));
        menus.add(MetaMenu.createEruptClassMenu(LandingAccessLog.class, root, 9));

        return menus;
    }
}
