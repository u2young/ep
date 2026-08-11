package xyz.herz.ep.erp.entity.stock;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 库存余额表(聚合视图)。
 * 唯一键 (product_id, sku_id, warehouse_id, batch_no);未启用批次时 batch_no='-'。
 */
@Getter @Setter
@Entity
@Table(name = "erp_stock_balance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "sku_id", "warehouse_id", "batch_no"}))
@Erupt(name = "库存余额",
    dataProxy = xyz.herz.ep.erp.core.ErpStockBalanceProxy.class)
public class ErpStockBalance extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", show = false, type = EditType.REFERENCE_TABLE))
    private ErpProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @EruptField(views = @View(title = "分类", column = "name"),
                edit = @Edit(title = "分类", show = false))
    private ErpProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id")
    @EruptField(views = @View(title = "SKU 编码", column = "code"),
                edit = @Edit(title = "SKU 编码", show = false, type = EditType.REFERENCE_TABLE))
    private ErpProductSku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", show = false, type = EditType.REFERENCE_TABLE))
    private ErpWarehouse warehouse;

    @EruptField(views = @View(title = "批号"),
                edit = @Edit(title = "批号", show = false))
    @Column(length = 100, nullable = false)
    private String batchNo = "-";

    @EruptField(views = @View(title = "可用数量"))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "锁定数量"),
                edit = @Edit(title = "锁定数量", show = false))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal lockedQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "在途数量"),
                edit = @Edit(title = "在途数量", show = false))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal inTransitQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "移动加权平均单位成本"))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal avgCost = BigDecimal.ZERO;
}
