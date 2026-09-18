package xyz.herz.ep.proj.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;
import xyz.herz.ep.proj.entity.cashflow.ProjCashFlow;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaim;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.entity.template.ProjProjectTemplate;
import xyz.herz.ep.proj.entity.timesheet.ProjTimesheet;

/**
 * PROJ 项目模块菜单注册
 */
@Component
public class ProjModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(ProjModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("proj")
                .description("项目管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("proj", "项目管理", "fa fa-project-diagram", 36);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("proj-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/proj.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(ProjProjectTemplate.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(ProjProject.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(ProjTask.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(ProjTimesheet.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(ProjExpenseClaim.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(ProjActivityCost.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(ProjCashFlow.class, root, 7));
        return menus;
    }
}
