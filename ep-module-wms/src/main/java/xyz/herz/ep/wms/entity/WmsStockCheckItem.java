package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 盘点明细行。 */
@Getter @Setter
@Entity
@Table(name = "wms_stock_check_item")
@Erupt(name = "盘点明细")
public class WmsStockCheckItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id")
    private WmsStockCheck check;

    @EruptField(views = @View(title = "库位 ID"),
                edit = @Edit(title = "库位 ID", notNull = true))
    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "账面数量"),
                edit = @Edit(title = "账面数量", show = false,
                    numberType = @NumberType(min = 0)))
    @Column(name = "book_qty", nullable = false)
    private Integer bookQty = 0;

    @EruptField(views = @View(title = "实盘数量"),
                edit = @Edit(title = "实盘数量", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "actual_qty", nullable = false)
    private Integer actualQty = 0;

    @EruptField(views = @View(title = "差异数量"),
                edit = @Edit(title = "差异数量", show = false))
    @Column(name = "diff_qty")
    private Integer diffQty;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
