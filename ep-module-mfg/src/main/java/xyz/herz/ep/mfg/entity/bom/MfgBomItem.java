package xyz.herz.ep.mfg.entity.bom;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * BOM 明细(参考 ERPNext BOM Item 子表)。
 * <p>extends {@link BaseModel} 子表,自带 id/createTime/updateTime。
 * <p>父关联由 {@link MfgBom#items} 的 @OneToMany @JoinColumn(name="bom_id") 单向维护,
 * 本实体无 bomId/bom 字段。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_bom_item")
public class MfgBomItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "product_id")
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProduct product;

    @EruptField(views = @View(title = "数量"),
                edit = @Edit(title = "数量", notNull = true, numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "unit_id")
    @EruptField(views = @View(title = "单位", column = "name"),
                edit = @Edit(title = "单位", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProductUnit unit;

    @EruptField(views = @View(title = "标准费率"),
                edit = @Edit(title = "标准费率", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal rate = BigDecimal.ZERO;

    @EruptField(views = @View(title = "金额"), edit = @Edit(title = "金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "库存单位"))
    @Column(name = "stock_uom", length = 40)
    private String stockUom;

    @EruptField(views = @View(title = "是否废品"),
                edit = @Edit(title = "是否废品", desc = "标记为产出废品"))
    @Column(name = "is_scrap", nullable = false)
    private Boolean isScrap = false;
}
