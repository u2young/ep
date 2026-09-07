package xyz.herz.ep.stk.entity.reconciliation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 库存盘点明细行(参考 ERPNext Stock Reconciliation Item)。
 * <p>记录物料编码/名称/账面数量/实际数量/差异;主单提交时回写差异合计 = 实际 - 账面。
 */
@Getter @Setter
@Entity
@Table(name = "stk_reconciliation_item")
public class StkReconciliationItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    @EruptField(edit = @Edit(title = "盘点单", show = false))
    private StkStockReconciliation reconciliation;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码", notNull = true))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"),
                edit = @Edit(title = "物料名称", notNull = true))
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @EruptField(views = @View(title = "账面数量"),
                edit = @Edit(title = "账面数量", notNull = true, desc = "系统现有库存"))
    @Column(name = "book_qty", nullable = false, precision = 18, scale = 2)
    private BigDecimal bookQty;

    @EruptField(views = @View(title = "实际数量"),
                edit = @Edit(title = "实际数量", notNull = true, desc = "盘点实数"))
    @Column(name = "actual_qty", nullable = false, precision = 18, scale = 2)
    private BigDecimal actualQty;

    @EruptField(views = @View(title = "差异"),
                edit = @Edit(title = "差异", show = false, desc = "提交时回写:实际 - 账面"))
    @Column(name = "variance", precision = 18, scale = 2)
    private BigDecimal variance;

    @EruptField(views = @View(title = "批次号"))
    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}
