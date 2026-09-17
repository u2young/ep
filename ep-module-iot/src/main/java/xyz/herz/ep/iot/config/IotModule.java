package xyz.herz.ep.iot.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.iot.entity.IotAlarm;
import xyz.herz.ep.iot.entity.IotAlarmLog;
import xyz.herz.ep.iot.entity.IotAlarmRule;
import xyz.herz.ep.iot.entity.IotDevice;
import xyz.herz.ep.iot.entity.IotDeviceMessage;
import xyz.herz.ep.iot.entity.IotProduct;
import xyz.herz.ep.iot.entity.IotProductCategory;
import xyz.herz.ep.iot.entity.IotThingModel;

/**
 * IoT模块菜单注册
 */
@Component
public class IotModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(IotModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("iot")
                .description("物联网管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("iot", "IoT物联网", "fa fa-flash", 20);
        menus.add(root);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("iot-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/iot.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(IotProduct.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(IotProductCategory.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(IotThingModel.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(IotDevice.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(IotDeviceMessage.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(IotAlarmRule.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(IotAlarm.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(IotAlarmLog.class, root, 8));

        return menus;
    }
}
