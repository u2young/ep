package xyz.herz.ep.stk.entity.reconciliation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.stk.core.StkEnumChoiceFetchHandler;
import xyz.herz.ep.stk.core.StkStateDataProxy;
import xyz.herz.ep.stk.enums.StkDictEnums.ReconciliationStatus;
import xyz.herz.ep.stk.handler.reconciliation.StkReconciliationLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存盘点单(参考 ERPNext Stock Reconciliation DocType)。
 * <p>状态机:0草稿/1已提交/2已取消。
 * 按仓库维度盘点,明细行记录物料编码/账面数量/实际数量/差异;提交时回写差异合计。
 */
@Getter @Setter
@Entity
@Table(name = "stk_reconciliation")
@Erupt(
    name = "库存盘点单",
    power = @Power(importable = true, export = true),
    dataProxy = StkStockReconciliation.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = StkReconciliationLifecycleHandler.CODE_SUBMIT,
            operationHandler = StkReconciliationLifecycleHandler.class),
        @RowOperation(title = "取消", code = StkReconciliationLifecycleHandler.CODE_CANCEL,
            operationHandler = StkReconciliationLifecycleHandler.class)
    }
)
public class StkStockReconciliation extends MetaModelVo {

    @EruptField(views = @View(title = "盘点单号"),
                edit = @Edit(title = "盘点单号", notNull = true, search = @Search))
    @Column(name = "reconciliation_no", length = 50, nullable = false)
    private String reconciliationNo;

    @EruptField(views = @View(title = "仓库编码"),
                edit = @Edit(title = "仓库编码", notNull = true, search = @Search))
    @Column(name = "warehouse_code", length = 50, nullable = false)
    private String warehouseCode;

    @EruptField(views = @View(title = "仓库名称"))
    @Column(name = "warehouse_name", length = 100)
    private String warehouseName;

    @EruptField(views = @View(title = "盘点日期"),
                edit = @Edit(title = "盘点日期", notNull = true))
    @Column(name = "reconcile_date", nullable = false)
    private LocalDate reconcileDate;

    @EruptField(views = @View(title = "账面合计"),
                edit = @Edit(title = "账面合计", show = false, desc = "提交时回写"))
    @Column(name = "total_book_qty", precision = 18, scale = 2)
    private BigDecimal totalBookQty;

    @EruptField(views = @View(title = "实际合计"),
                edit = @Edit(title = "实际合计", show = false, desc = "提交时回写"))
    @Column(name = "total_actual_qty", precision = 18, scale = 2)
    private BigDecimal totalActualQty;

    @EruptField(views = @View(title = "差异合计"))
    @Column(name = "total_variance", precision = 18, scale = 2)
    private BigDecimal totalVariance;

    @EruptField(views = @View(title = "提交时间"),
                edit = @Edit(title = "提交时间", show = false, desc = "提交时回写"))
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @EruptField(views = @View(title = "盘点人"))
    @Column(name = "reconciler", length = 50)
    private String reconciler;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = StkEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ReconciliationStatus")))
    @Column(nullable = false)
    private Integer status = ReconciliationStatus.DRAFT.code;

    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "盘点明细", type = EditType.TAB_TABLE_ADD))
    private List<StkReconciliationItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends StkStateDataProxy<StkStockReconciliation> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
