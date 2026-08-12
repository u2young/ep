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

/** 上架明细行。 */
@Getter @Setter
@Entity
@Table(name = "wms_putaway_item")
@Erupt(name = "上架明细")
public class WmsPutawayItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "putaway_id")
    private WmsPutaway putaway;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "目标库位 ID"),
                edit = @Edit(title = "目标库位 ID", notNull = true))
    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @EruptField(views = @View(title = "上架数量"),
                edit = @Edit(title = "上架数量", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "qty", nullable = false)
    private Integer qty = 0;
}
