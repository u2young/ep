package xyz.herz.ep.mfg.entity.stockentry;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.StockEntryStatus;
import xyz.herz.ep.mfg.enums.MfgDictEnums.StockEntryType;
import xyz.herz.ep.mfg.handler.stockentry.MfgStockEntryAuditHandler;
import xyz.herz.ep.mfg.handler.stockentry.MfgStockEntryCancelHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产领料/完工入库单(参考 ERPNext Stock Entry DocType)。
 * <p>状态机:DRAFT(0) → AUDITED(1,触发 InventoryChangeFacade 库存联动) / CANCELLED(2)。
 * 只有 DRAFT 允许表单直改;审核/取消走行按钮。
 * <p>ttype 决定业务方向:领料(MATERIAL_ISSUE)出库 / 完工入库(COMPLETION_IN)入库 / 委外发收。
 * items 为明细(MfgStockEntryItem)。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_stock_entry")
@Erupt(
    name = "生产出入库单",
    power = @Power(importable = true, export = true),
    dataProxy = MfgStockEntry.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审核", code = MfgStockEntryAuditHandler.CODE_AUDIT, icon = "fa fa-check-circle",
            operationHandler = MfgStockEntryAuditHandler.class, operationParam = { MfgStockEntryAuditHandler.CODE_AUDIT }),
        @RowOperation(title = "取消", code = MfgStockEntryCancelHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = MfgStockEntryCancelHandler.class, operationParam = { MfgStockEntryCancelHandler.CODE_CANCEL })
    }
)
public class MfgStockEntry extends MetaModelVo {

    @EruptField(views = @View(title = "单号"),
                edit = @Edit(title = "单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "单据类型"),
                edit = @Edit(title = "单据类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockEntryType")))
    @Column(name = "ttype", nullable = false)
    private Integer ttype = StockEntryType.MATERIAL_ISSUE.code;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "work_order_id")
    @EruptField(views = @View(title = "工单", column = "no"),
                edit = @Edit(title = "工单", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private MfgWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "from_warehouse_id")
    @EruptField(views = @View(title = "来源仓", column = "name"),
                edit = @Edit(title = "来源仓", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "to_warehouse_id")
    @EruptField(views = @View(title = "目标仓", column = "name"),
                edit = @Edit(title = "目标仓", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse toWarehouse;

    @EruptField(views = @View(title = "总数量"), edit = @Edit(title = "总数量", show = false))
    @Column(name = "total_qty", precision = 24, scale = 6)
    private BigDecimal totalQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "总金额"), edit = @Edit(title = "总金额", show = false))
    @Column(name = "total_amount", precision = 24, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockEntryStatus")))
    @Column(nullable = false)
    private Integer status = StockEntryStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "stock_entry_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "出入库明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<MfgStockEntryItem> items = new ArrayList<>();

    public static class Proxy extends MfgStateDataProxy<MfgStockEntry> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ StockEntryStatus.DRAFT.code, null };
        }
    }
}
