package xyz.herz.ep.mfg.entity.jobcard;

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
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.entity.operation.MfgOperation;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.JobCardStatus;
import xyz.herz.ep.mfg.handler.jobcard.MfgJobCardLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派工单(参考 ERPNext Job Card DocType)。
 * <p>状态机:PENDING(0) → IN_PRODUCTION(1) → COMPLETED(2) / CANCELLED(3)。
 * 只有 PENDING 允许表单直改;其他状态走行按钮(开工/完工/取消)。
 * <p>工时回写:开工记 startedAt,完工记 completedAt + totalTimeInMins(分)。
 * 员工用 employeeId + employeeName 快照(不 REF 跨模块 UPMS)。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_job_card")
@Erupt(
    name = "派工单",
    power = @Power(importable = true, export = true),
    dataProxy = MfgJobCard.Proxy.class,
    rowOperation = {
        @RowOperation(title = "开工", code = MfgJobCardLifecycleHandler.CODE_START, icon = "fa fa-play",
            operationHandler = MfgJobCardLifecycleHandler.class, operationParam = { MfgJobCardLifecycleHandler.CODE_START }),
        @RowOperation(title = "完工", code = MfgJobCardLifecycleHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = MfgJobCardLifecycleHandler.class, operationParam = { MfgJobCardLifecycleHandler.CODE_COMPLETE }),
        @RowOperation(title = "取消", code = MfgJobCardLifecycleHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = MfgJobCardLifecycleHandler.class, operationParam = { MfgJobCardLifecycleHandler.CODE_CANCEL })
    }
)
public class MfgJobCard extends MetaModelVo {

    @EruptField(views = @View(title = "派工单号"),
                edit = @Edit(title = "派工单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "work_order_id")
    @EruptField(views = @View(title = "工单", column = "no"),
                edit = @Edit(title = "工单", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private MfgWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "operation_id")
    @EruptField(views = @View(title = "工序", column = "name"),
                edit = @Edit(title = "工序", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private MfgOperation operation;

    @EruptField(views = @View(title = "员工 ID"), edit = @Edit(title = "员工 ID", show = false))
    @Column(name = "employee_id")
    private Long employeeId;

    @EruptField(views = @View(title = "员工名称"),
                edit = @Edit(title = "员工名称"))
    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @EruptField(views = @View(title = "工时(分)"), edit = @Edit(title = "工时(分)", show = false))
    @Column(name = "total_time_in_mins", precision = 24, scale = 6)
    private BigDecimal totalTimeInMins = BigDecimal.ZERO;

    @EruptField(views = @View(title = "完工数"), edit = @Edit(title = "完工数", show = false))
    @Column(name = "completed_qty", precision = 24, scale = 6)
    private BigDecimal completedQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "不良数"), edit = @Edit(title = "不良数", show = false))
    @Column(name = "rejected_qty", precision = 24, scale = 6)
    private BigDecimal rejectedQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "开工时间"), edit = @Edit(title = "开工时间", show = false))
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @EruptField(views = @View(title = "完工时间"), edit = @Edit(title = "完工时间", show = false))
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "JobCardStatus")))
    @Column(nullable = false)
    private Integer status = JobCardStatus.PENDING.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends MfgStateDataProxy<MfgJobCard> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ JobCardStatus.PENDING.code, null };
        }
    }
}
