package xyz.herz.ep.mfg.entity.workorder;

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
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.entity.bom.MfgBom;
import xyz.herz.ep.mfg.enums.MfgDictEnums.WorkOrderStatus;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderCancelHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderCompleteHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderStartHandler;
import xyz.herz.ep.mfg.handler.workorder.MfgWorkOrderStopHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工单(参考 ERPNext Work Order DocType)。
 * <p>状态机:DRAFT(0) → NOT_STARTED(1) → IN_PRODUCTION(2) → COMPLETED(3) / STOPPED(4) / CANCELLED(5)。
 * 只有 DRAFT 允许表单直改;其他状态走行按钮(开工/完工/停工/取消)。
 * <p>producedQty 为派生字段(由完工入库 StockEntry 回写);成本中心用快照(不 REF fin 模块)。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_work_order")
@Erupt(
    name = "工单",
    power = @Power(importable = true, export = true),
    dataProxy = MfgWorkOrder.Proxy.class,
    rowOperation = {
        @RowOperation(title = "开工", code = MfgWorkOrderStartHandler.CODE_START, icon = "fa fa-play",
            operationHandler = MfgWorkOrderStartHandler.class, operationParam = { MfgWorkOrderStartHandler.CODE_START }),
        @RowOperation(title = "完工", code = MfgWorkOrderCompleteHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = MfgWorkOrderCompleteHandler.class, operationParam = { MfgWorkOrderCompleteHandler.CODE_COMPLETE }),
        @RowOperation(title = "停工", code = MfgWorkOrderStopHandler.CODE_STOP, icon = "fa fa-pause",
            operationHandler = MfgWorkOrderStopHandler.class, operationParam = { MfgWorkOrderStopHandler.CODE_STOP }),
        @RowOperation(title = "取消", code = MfgWorkOrderCancelHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = MfgWorkOrderCancelHandler.class, operationParam = { MfgWorkOrderCancelHandler.CODE_CANCEL })
    }
)
public class MfgWorkOrder extends MetaModelVo {

    @EruptField(views = @View(title = "工单号"),
                edit = @Edit(title = "工单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "product_id")
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProduct product;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "bom_id")
    @EruptField(views = @View(title = "BOM", column = "no"),
                edit = @Edit(title = "BOM", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private MfgBom bom;

    @EruptField(views = @View(title = "计划数量"),
                edit = @Edit(title = "计划数量", notNull = true, numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已完工数"), edit = @Edit(title = "已完工数", show = false))
    @Column(name = "produced_qty", precision = 24, scale = 6)
    private BigDecimal producedQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "计划开工"),
                edit = @Edit(title = "计划开工",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "planned_start")
    private LocalDate plannedStart;

    @EruptField(views = @View(title = "计划完工"),
                edit = @Edit(title = "计划完工",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "planned_end")
    private LocalDate plannedEnd;

    @EruptField(views = @View(title = "实际开工"), edit = @Edit(title = "实际开工", show = false))
    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @EruptField(views = @View(title = "实际完工"), edit = @Edit(title = "实际完工", show = false))
    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_warehouse_id")
    @EruptField(views = @View(title = "领料仓", column = "name"),
                edit = @Edit(title = "领料仓", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "target_warehouse_id")
    @EruptField(views = @View(title = "入库仓", column = "name"),
                edit = @Edit(title = "入库仓", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse targetWarehouse;

    @EruptField(views = @View(title = "成本中心 ID"), edit = @Edit(title = "成本中心 ID", show = false))
    @Column(name = "cost_center_id")
    private Long costCenterId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "WorkOrderStatus")))
    @Column(nullable = false)
    private Integer status = WorkOrderStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends MfgStateDataProxy<MfgWorkOrder> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ WorkOrderStatus.DRAFT.code, null };
        }
    }
}
