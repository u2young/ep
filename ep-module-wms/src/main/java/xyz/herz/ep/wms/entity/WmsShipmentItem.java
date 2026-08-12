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

/** 出库通知明细行。 */
@Getter @Setter
@Entity
@Table(name = "wms_shipment_item")
@Erupt(name = "出库通知明细")
public class WmsShipmentItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private WmsShipmentNotice notice;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "SKU 名称"),
                edit = @Edit(title = "SKU 名称"))
    @Column(name = "sku_name", length = 200)
    private String skuName;

    @EruptField(views = @View(title = "预期数量"),
                edit = @Edit(title = "预期数量", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "expected_qty", nullable = false)
    private Integer expectedQty = 0;

    @EruptField(views = @View(title = "已拣数量"),
                edit = @Edit(title = "已拣数量", show = false))
    @Column(name = "picked_qty", nullable = false)
    private Integer pickedQty = 0;
}
