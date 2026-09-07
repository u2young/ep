package xyz.herz.ep.mfg.entity.stockentry;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.entity.product.ErpProduct;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 生产出入库明细(参考 ERPNext Stock Entry Detail 子表)。
 * <p>extends {@link BaseModel} 子表,自带 id/createTime/updateTime。
 * <p>父关联由 {@link MfgStockEntry#items} 的 @OneToMany @JoinColumn(name="stock_entry_id") 单向维护,
 * 本实体无 stockEntryId/stockEntry 字段。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_stock_entry_item")
public class MfgStockEntryItem extends BaseModel {

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

    @EruptField(views = @View(title = "成本单价"),
                edit = @Edit(title = "成本单价", numberType = @NumberType(min = 0)))
    @Column(name = "valuation_rate", precision = 24, scale = 6)
    private BigDecimal valuationRate = BigDecimal.ZERO;

    @EruptField(views = @View(title = "金额"), edit = @Edit(title = "金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
