package xyz.herz.ep.stk.entity.entry;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 库存出入库明细行(参考 ERPNext Stock Entry Detail)。
 * <p>记录物料编码/名称/数量/单价/批次号/序列号;主单提交时回写 totalQty/totalAmount。
 */
@Getter @Setter
@Entity
@Table(name = "stk_entry_item")
public class StkStockEntryItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    @EruptField(edit = @Edit(title = "出入库单", show = false))
    private StkStockEntry entry;

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

    @EruptField(views = @View(title = "单价"))
    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @EruptField(views = @View(title = "小计"),
                edit = @Edit(title = "小计", show = false))
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @EruptField(views = @View(title = "批次号"),
                edit = @Edit(title = "批次号", desc = "可空:无批次管理时留空"))
    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @EruptField(views = @View(title = "序列号"),
                edit = @Edit(title = "序列号", desc = "可空:无序列号管理时留空"))
    @Column(name = "serial_no", length = 100)
    private String serialNo;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}
