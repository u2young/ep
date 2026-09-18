package xyz.herz.ep.qal.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.qal.entity.criteria.QalCriteria;
import xyz.herz.ep.qal.entity.feedback.QalFeedback;
import xyz.herz.ep.qal.entity.inspection.QalInspection;
import xyz.herz.ep.qal.entity.nc.QalNonConformance;

/**
 * QAL 质量模块菜单注册
 */
@Component
public class QalModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(QalModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("qal")
                .description("质量管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("qal", "质量管理", "fa fa-check-double", 38);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("qal-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/qal.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(QalCriteria.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(QalInspection.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(QalNonConformance.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(QalFeedback.class, root, 4));
        return menus;
    }
}
