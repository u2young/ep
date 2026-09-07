package xyz.herz.ep.pur.entity.receipt;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 采购收货明细行(参考 ERPNext Purchase Receipt Item)。
 * <p>记录收货物料编码/名称/应收数量/实收数量/单价;主单收货时回写 receivedQty/receivedAmount。
 */
@Getter @Setter
@Entity
@Table(name = "pur_receipt_item")
public class PurReceiptItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    @EruptField(edit = @Edit(title = "收货单", show = false))
    private PurPurchaseReceipt receipt;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码", notNull = true))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"),
                edit = @Edit(title = "物料名称", notNull = true))
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @EruptField(views = @View(title = "应收数量"),
                edit = @Edit(title = "应收数量", notNull = true))
    @Column(name = "ordered_qty", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderedQty;

    @EruptField(views = @View(title = "实收数量"),
                edit = @Edit(title = "实收数量", desc = "收货时填写"))
    @Column(name = "received_qty", precision = 18, scale = 2)
    private BigDecimal receivedQty;

    @EruptField(views = @View(title = "单价"),
                edit = @Edit(title = "单价"))
    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @EruptField(views = @View(title = "小计"),
                edit = @Edit(title = "小计", show = false))
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}
