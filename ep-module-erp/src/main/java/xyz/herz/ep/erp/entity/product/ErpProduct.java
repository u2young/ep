package xyz.herz.ep.erp.entity.product;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.core.ErpStateDataProxy;
import xyz.herz.ep.erp.entity.master.ErpProductBrand;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.enums.ErpDictEnums.CostMethod;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.ProductListingStatus;
import xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品档案(SPU 级,1 ↔ N SKU)。
 * <p>单据明细和库存统一落在 SKU 维度;
 * {@link #status} 主数据启停、{@link #listingStatus} 销售上下架,都通过行按钮切换。
 */
@Getter @Setter
@Entity
@Table(name = "erp_product")
@Erupt(
    name = "产品档案",
    dataProxy = ErpProduct.Proxy.class,
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "上架销售", code = ErpMasterToggleHandler.LIST, icon = "fa fa-cloud-upload", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.LIST }),
        @RowOperation(title = "下架销售", code = ErpMasterToggleHandler.DELIST, icon = "fa fa-cloud-download", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.DELIST }),
        @RowOperation(title = "启用", code = ErpMasterToggleHandler.ENABLE, icon = "fa fa-check", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = ErpMasterToggleHandler.DISABLE, icon = "fa fa-ban", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.DISABLE })
    }
)
public class ErpProduct extends MetaModelVo {

    @EruptField(views = @View(title = "产品名称"),
                edit = @Edit(title = "产品名称", notNull = true))
    @Column(length = 255, nullable = false)
    private String name;

    @EruptField(views = @View(title = "产品编码"),
                edit = @Edit(title = "产品编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @EruptField(views = @View(title = "分类", column = "name"),
                edit = @Edit(title = "分类", type = EditType.REFERENCE_TREE,
                    referenceTreeType = @ReferenceTreeType(id = "id", label = "name")))
    private ErpProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @EruptField(views = @View(title = "品牌", column = "name"),
                edit = @Edit(title = "品牌", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProductBrand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    @EruptField(views = @View(title = "计量单位", column = "name"),
                edit = @Edit(title = "计量单位", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProductUnit unit;

    @EruptField(views = @View(title = "主条码"),
                edit = @Edit(title = "主条码"))
    @Column(length = 100)
    private String barCode;

    @EruptField(views = @View(title = "规格型号"),
                edit = @Edit(title = "规格型号"))
    @Column(length = 200)
    private String spec;

    @EruptField(views = @View(title = "产品图"),
                edit = @Edit(title = "产品图", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String picUrl;

    @EruptField(views = @View(title = "标签"),
                edit = @Edit(title = "标签", type = EditType.TAGS,
                    tagsType = @TagsType))
    @Column(length = 500)
    private String tags;

    @EruptField(views = @View(title = "标准进价"),
                edit = @Edit(title = "标准进价", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "标准售价"),
                edit = @Edit(title = "标准售价", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "最低售价"),
                edit = @Edit(title = "最低售价", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal minSalePrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "成本核算方法"),
                edit = @Edit(title = "成本核算方法", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "CostMethod")))
    private Integer costMethod = CostMethod.WEIGHTED_AVG.code;

    @EruptField(views = @View(title = "启用批次管理"),
                edit = @Edit(title = "启用批次管理"))
    private Boolean enableBatch = false;

    @EruptField(views = @View(title = "启用保质期"),
                edit = @Edit(title = "启用保质期"))
    private Boolean enableExpiry = false;

    @EruptField(views = @View(title = "保质期天数"),
                edit = @Edit(title = "保质期天数", numberType = @NumberType(min = 0)))
    private Integer expiryDays = 0;

    @EruptField(views = @View(title = "启用序列号"),
                edit = @Edit(title = "启用序列号"))
    private Boolean enableSerial = false;

    @EruptField(views = @View(title = "销售上下架状态"),
                edit = @Edit(title = "销售上下架状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ProductListingStatus")))
    private Integer listingStatus = ProductListingStatus.LISTED.code;

    @EruptField(views = @View(title = "启停状态"),
                edit = @Edit(title = "启停状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    @EruptField(edit = @Edit(title = "SKU 规格/条码", type = EditType.TAB_TABLE_ADD))
    private List<ErpProductSku> skus = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    @EruptField(edit = @Edit(title = "多条码", type = EditType.TAB_TABLE_ADD))
    private List<ErpProductBarCode> barCodes = new ArrayList<>();

    /** 锁定两个状态字段:只能按钮改,不能表单直接写。同时回绑 OneToMany 子项的 product 引用。 */
    public static class Proxy extends ErpStateDataProxy<ErpProduct> {
        @Override protected String stateFieldName() { return "status,listingStatus"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ null };
        }

        private void bindChildren(ErpProduct p) {
            if (p.getSkus() != null) {
                for (ErpProductSku s : p.getSkus()) {
                    if (s.getProduct() == null) s.setProduct(p);
                }
            }
            if (p.getBarCodes() != null) {
                for (ErpProductBarCode b : p.getBarCodes()) {
                    if (b.getProduct() == null) b.setProduct(p);
                }
            }
        }

        @Override public void beforeAdd(ErpProduct p) { bindChildren(p); super.beforeAdd(p); }
        @Override public void beforeUpdate(ErpProduct p) { bindChildren(p); super.beforeUpdate(p); }
    }
}
