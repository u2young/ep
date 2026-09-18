package xyz.herz.ep.ast.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;
import xyz.herz.ep.ast.entity.depreciation.AstDepreciationSchedule;
import xyz.herz.ep.ast.entity.location.AstLocation;
import xyz.herz.ep.ast.entity.movement.AstAssetMovement;
import xyz.herz.ep.ast.entity.repair.AstAssetRepair;

/**
 * AST 固定资产模块菜单注册
 */
@Component
public class AstModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(AstModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("ast")
                .description("固定资产管理")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("ast", "固定资产管理", "fa fa-cubes", 31);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("ast-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/ast.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(AstAssetCategory.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(AstLocation.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(AstAsset.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(AstAssetMovement.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(AstAssetRepair.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(AstDepreciationSchedule.class, root, 6));
        return menus;
    }
}
