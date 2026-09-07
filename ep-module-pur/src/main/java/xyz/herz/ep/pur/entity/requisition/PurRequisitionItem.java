package xyz.herz.ep.pur.entity.requisition;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 请购单明细行(参考 ERPNext Material Request Item)。
 * <p>记录请购物料编码/名称/数量/预估单价;合计回写主单 totalAmount。
 */
@Getter @Setter
@Entity
@Table(name = "pur_requisition_item")
public class PurRequisitionItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    @EruptField(edit = @Edit(title = "请购单", show = false))
    private PurPurchaseRequisition requisition;

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
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal qty;

    @EruptField(views = @View(title = "预估单价"),
                edit = @Edit(title = "预估单价"))
    @Column(name = "estimated_price", precision = 18, scale = 2)
    private BigDecimal estimatedPrice;

    @EruptField(views = @View(title = "小计"),
                edit = @Edit(title = "小计", show = false))
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}
