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

/** 收货明细行。 */
@Getter @Setter
@Entity
@Table(name = "wms_receipt_item")
@Erupt(name = "收货明细")
public class WmsReceiptItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id")
    private WmsReceipt receipt;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "收货数量"),
                edit = @Edit(title = "收货数量", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0;
}
