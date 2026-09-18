package xyz.herz.ep.erp.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import xyz.erupt.core.constant.MenuStatus;
import xyz.erupt.core.constant.MenuTypeEnum;
import xyz.erupt.core.module.EruptModule;
import xyz.erupt.core.module.EruptModuleInvoke;
import xyz.erupt.core.module.MetaMenu;
import xyz.herz.ep.erp.entity.document.ErpStockIn;
import xyz.herz.ep.erp.entity.document.ErpStockOut;
import xyz.herz.ep.erp.entity.finance.ErpFinancePayment;
import xyz.herz.ep.erp.entity.finance.ErpFinanceReceipt;
import xyz.herz.ep.erp.entity.finance.ErpFinanceSettlement;
import xyz.herz.ep.erp.entity.master.ErpAccount;
import xyz.herz.ep.erp.entity.master.ErpCustomer;
import xyz.herz.ep.erp.entity.master.ErpProductBrand;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseIn;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.sale.ErpSaleOrder;
import xyz.herz.ep.erp.entity.sale.ErpSaleOut;
import xyz.herz.ep.erp.entity.stock.ErpStockBalance;

/**
 * ERP 进销存模块菜单注册
 */
@Component
public class ErpModule implements EruptModule {

    static {
        EruptModuleInvoke.addEruptModule(ErpModule.class);
    }

    @Override
    public xyz.erupt.core.module.ModuleInfo info() {
        return xyz.erupt.core.module.ModuleInfo.builder()
                .name("erp")
                .description("ERP 进销存模块")
                .build();
    }

    @Override
    public List<MetaMenu> initMenus() {
        List<MetaMenu> menus = new ArrayList<>();
        MetaMenu root = MetaMenu.createRootMenu("erp", "ERP 进销存", "fa fa-boxes", 11);
        menus.add(root);

        MetaMenu guide = new MetaMenu();
        guide.setCode("erp-guide");
        guide.setName("使用指南");
        guide.setIcon("fa fa-book");
        guide.setValue("/guide/erp.html");
        guide.setType(MenuTypeEnum.NEW_WINDOW.getCode());
        guide.setStatus(MenuStatus.OPEN);
        guide.setParentMenu(root);
        guide.setSort(0);
        menus.add(guide);

        menus.add(MetaMenu.createEruptClassMenu(ErpProductCategory.class, root, 1));
        menus.add(MetaMenu.createEruptClassMenu(ErpProductUnit.class, root, 2));
        menus.add(MetaMenu.createEruptClassMenu(ErpProductBrand.class, root, 3));
        menus.add(MetaMenu.createEruptClassMenu(ErpWarehouse.class, root, 4));
        menus.add(MetaMenu.createEruptClassMenu(ErpSupplier.class, root, 5));
        menus.add(MetaMenu.createEruptClassMenu(ErpCustomer.class, root, 6));
        menus.add(MetaMenu.createEruptClassMenu(ErpAccount.class, root, 7));
        menus.add(MetaMenu.createEruptClassMenu(ErpProduct.class, root, 8));
        menus.add(MetaMenu.createEruptClassMenu(ErpProductSku.class, root, 9));
        menus.add(MetaMenu.createEruptClassMenu(ErpStockBalance.class, root, 10));
        menus.add(MetaMenu.createEruptClassMenu(ErpStockIn.class, root, 11));
        menus.add(MetaMenu.createEruptClassMenu(ErpStockOut.class, root, 12));
        menus.add(MetaMenu.createEruptClassMenu(ErpPurchaseOrder.class, root, 13));
        menus.add(MetaMenu.createEruptClassMenu(ErpPurchaseIn.class, root, 14));
        menus.add(MetaMenu.createEruptClassMenu(ErpSaleOrder.class, root, 15));
        menus.add(MetaMenu.createEruptClassMenu(ErpSaleOut.class, root, 16));
        menus.add(MetaMenu.createEruptClassMenu(ErpFinancePayment.class, root, 17));
        menus.add(MetaMenu.createEruptClassMenu(ErpFinanceReceipt.class, root, 18));
        menus.add(MetaMenu.createEruptClassMenu(ErpFinanceSettlement.class, root, 19));
        return menus;
    }
}
