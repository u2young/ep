package xyz.herz.ep.mp.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.mp.entity.MpAccount;
import xyz.herz.ep.mp.entity.MpAutoReply;
import xyz.herz.ep.mp.entity.MpMaterial;
import xyz.herz.ep.mp.entity.MpMenu;
import xyz.herz.ep.mp.entity.MpMessage;
import xyz.herz.ep.mp.entity.MpUser;
import xyz.herz.ep.mp.entity.MpUserTag;

/**
 * 微信公众号模块菜单注册
 */
@Component
public class MpModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(MpModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("mp")
                .description("微信公众号管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("mp", "微信公众号", "fa fa-wechat", 50);
        menus.add(root);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("mp-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/mp.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(MpAccount.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(MpMenu.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(MpAutoReply.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(MpMaterial.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(MpMessage.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(MpUser.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(MpUserTag.class, root, 7));

        return menus;
    }
}
