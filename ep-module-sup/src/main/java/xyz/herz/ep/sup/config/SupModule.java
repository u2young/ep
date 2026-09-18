package xyz.herz.ep.sup.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.sup.entity.assignment.SupIssueAssignment;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sup.entity.kb.SupKnowledgeBase;
import xyz.herz.ep.sup.entity.priority.SupIssuePriority;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;
import xyz.herz.ep.sup.entity.sla.SupServiceLevelAgreement;

/**
 * SUP 服务支持模块菜单注册
 */
@Component
public class SupModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(SupModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("sup")
                .description("服务支持管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("sup", "服务支持管理", "fa fa-life-ring", 39);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("sup-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/sup.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(SupIssuePriority.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(SupServiceLevelAgreement.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(SupIssue.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(SupIssueAssignment.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(SupKnowledgeBase.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(SupSupportSettings.class, root, 6));
        return menus;
    }
}
