package xyz.herz.ep.sal.entity.deliverynote;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 送货单行明细(参考 ERPNext Delivery Note Item)。
 * <p>记录物料编码/名称/数量/单价/金额;关联送货单。
 */
@Getter @Setter
@Entity
@Table(name = "sal_delivery_note_item")
public class SalDeliveryNoteItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    @EruptField(edit = @Edit(title = "送货单", show = false))
    private SalDeliveryNote deliveryNote;

    @EruptField(views = @View(title = "物料编码"),
            edit = @Edit(title = "物料编码", notNull = true))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"),
            edit = @Edit(title = "物料名称", notNull = true))
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @EruptField(views = @View(title = "数量"),
            edit = @Edit(title = "数量", notNull = true))
    @Column(name = "qty", precision = 18, scale = 2, nullable = false)
    private BigDecimal qty;

    @EruptField(views = @View(title = "单价"),
            edit = @Edit(title = "单价"))
    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @EruptField(views = @View(title = "金额"),
            edit = @Edit(title = "金额", show = false))
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}