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

/** 移库明细行。 */
@Getter @Setter
@Entity
@Table(name = "wms_stock_move_order_item")
@Erupt(name = "移库明细")
public class WmsStockMoveOrderItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "move_order_id")
    private WmsStockMoveOrder order;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "源库位 ID"),
                edit = @Edit(title = "源库位 ID", notNull = true))
    @Column(name = "from_location_id", nullable = false)
    private Long fromLocationId;

    @EruptField(views = @View(title = "目标库位 ID"),
                edit = @Edit(title = "目标库位 ID", notNull = true))
    @Column(name = "to_location_id", nullable = false)
    private Long toLocationId;

    @EruptField(views = @View(title = "移库数量"),
                edit = @Edit(title = "移库数量", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "qty", nullable = false)
    private Integer qty = 0;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
