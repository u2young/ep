package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTreeType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.core.MallStateDataProxy;
import xyz.herz.ep.mall.enums.MallDictEnums.ListingStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品 SPU(1 ↔ N SKU)。
 * <p>{@link #status} 为上下架状态,后台可直接编辑(上下架)。
 */
@Getter @Setter
@Entity
@Table(name = "mall_product_spu")
@Erupt(
    name = "商品SPU",
    dataProxy = MallProductSpu.Proxy.class,
    power = @Power(importable = true, export = true)
)
public class MallProductSpu extends BaseModel {

    @EruptField(views = @View(title = "商品名称"),
                edit = @Edit(title = "商品名称", notNull = true))
    @Column(length = 255, nullable = false)
    private String name;

    @EruptField(views = @View(title = "商品编码"),
                edit = @Edit(title = "商品编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @EruptField(views = @View(title = "分类", column = "name"),
                edit = @Edit(title = "分类", type = EditType.REFERENCE_TREE,
                    referenceTreeType = @ReferenceTreeType(id = "id", label = "name")))
    private MallProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    @EruptField(views = @View(title = "品牌", column = "name"),
                edit = @Edit(title = "品牌", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private MallProductBrand brand;

    @EruptField(views = @View(title = "主图"),
                edit = @Edit(title = "主图", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String picUrl;

    @EruptField(views = @View(title = "商品详情"),
                edit = @Edit(title = "商品详情", type = EditType.TEXTAREA))
    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    @EruptField(views = @View(title = "上下架状态"),
                edit = @Edit(title = "上下架状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ListingStatus")))
    private Integer status = ListingStatus.LISTED.code;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    @EruptField(edit = @Edit(title = "SKU 规格", type = EditType.TAB_TABLE_ADD))
    private List<MallProductSku> skus = new ArrayList<>();

    /** 回绑子项的 product 引用,保持双向一致性。上下架状态允许直接编辑。 */
    public static class Proxy extends MallStateDataProxy<MallProductSpu> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ ListingStatus.LISTED.code, ListingStatus.DELISTED.code, null };
        }

        private void bindChildren(MallProductSpu p) {
            if (p.getSkus() != null) {
                for (MallProductSku s : p.getSkus()) {
                    if (s.getProduct() == null) s.setProduct(p);
                }
            }
        }

        @Override public void beforeAdd(MallProductSpu p) { bindChildren(p); }
        @Override public void beforeUpdate(MallProductSpu p) { bindChildren(p); super.beforeUpdate(p); }
    }
}
