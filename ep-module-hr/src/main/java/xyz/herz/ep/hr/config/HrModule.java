package xyz.herz.ep.hr.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.hr.entity.attendance.HrAttendance;
import xyz.herz.ep.hr.entity.department.HrDepartment;
import xyz.herz.ep.hr.entity.designation.HrDesignation;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.hr.entity.leavetype.HrLeaveType;
import xyz.herz.ep.hr.entity.leave.HrLeaveApplication;

/**
 * HR 人力资源模块菜单注册
 */
@Component
public class HrModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(HrModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("hr")
                .description("人力资源管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("hr", "人力资源管理", "fa fa-user-plus", 33);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("hr-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/hr.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(HrDepartment.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(HrDesignation.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(HrEmployee.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(HrLeaveType.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(HrAttendance.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(HrLeaveApplication.class, root, 6));
        return menus;
    }
}
