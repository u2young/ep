package xyz.herz.ep.crm.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.crm.entity.CrmBusiness;
import xyz.herz.ep.crm.entity.CrmBusinessStatus;
import xyz.herz.ep.crm.entity.CrmBusinessStatusType;
import xyz.herz.ep.crm.entity.CrmClue;
import xyz.herz.ep.crm.entity.CrmContact;
import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.entity.CrmCustomerPoolConfig;
import xyz.herz.ep.crm.entity.CrmFollowUpRecord;
import xyz.herz.ep.crm.entity.CrmReceivablePlan;
import xyz.herz.ep.crm.entity.CrmReceivableRecord;
import xyz.herz.ep.crm.entity.CrmTeamMember;

/**
 * CRM模块菜单注册
 */
@Component
public class CrmModule implements EruptModule {

    static {
        // 注册到 Erupt 模块调用链，使 initMenus() 在启动时被框架调用
        xyz.erupt.core.module.EruptModuleInvoke.addEruptModule(CrmModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("crm")
                .description("客户关系管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("crm", "CRM客户管理", "fa fa-users", 30);
        menus.add(root);

        // 使用指南：纯 HTML 页面，新窗口打开
        MetaMenu guide = new MetaMenu();
        guide.setCode("crm-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/crm.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(CrmCustomer.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(CrmContact.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(CrmClue.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(CrmBusiness.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(CrmBusinessStatusType.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(CrmBusinessStatus.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(CrmContract.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(CrmReceivablePlan.class, root, 8));
        menus.add(MetaMenu.createEruptClassMenu(CrmReceivableRecord.class, root, 9));
        menus.add(MetaMenu.createEruptClassMenu(CrmFollowUpRecord.class, root, 10));
        menus.add(MetaMenu.createEruptClassMenu(CrmTeamMember.class, root, 11));
        menus.add(MetaMenu.createEruptClassMenu(CrmCustomerPoolConfig.class, root, 12));

        return menus;
    }
}
