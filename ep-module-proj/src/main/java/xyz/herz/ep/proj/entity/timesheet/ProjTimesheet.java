package xyz.herz.ep.proj.entity.timesheet;

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
import xyz.herz.ep.proj.core.ProjEnumChoiceFetchHandler;
import xyz.herz.ep.proj.core.ProjStateDataProxy;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.enums.ProjDictEnums.BillingStatus;
import xyz.herz.ep.proj.handler.timesheet.ProjTimesheetApproveHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工时表(参考 ERPNext Timesheet DocType)。
 * <p>billingStatus: UNBILLED(0) → BILLED(1) → INVOICED(2)。
 * approve(0→1) 时:累加 ProjActivityCost(costType=LABOR) + 回写 ProjTask.actualTime +
 * ProjProject.totalCost,并通过 FinPostingFacade 触发 GL(I-11:借项目成本/贷应付薪酬)。
 * <p>hours = Duration.between(fromTime, toTime) 小时;cost = hours * billingRate。
 * 员工用快照(employeeId+employeeName)。
 */
@Getter @Setter
@Entity
@Table(name = "proj_timesheet")
@Erupt(
    name = "工时表",
    power = @Power(importable = true, export = true),
    dataProxy = ProjTimesheet.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审批", code = ProjTimesheetApproveHandler.CODE_APPROVE,
            operationHandler = ProjTimesheetApproveHandler.class,
            operationParam = { ProjTimesheetApproveHandler.CODE_APPROVE })
    }
)
public class ProjTimesheet extends MetaModelVo {

    @EruptField(views = @View(title = "员工ID"), edit = @Edit(title = "员工ID", show = false))
    @Column(name = "employee_id")
    private Long employeeId;

    @EruptField(views = @View(title = "员工姓名"),
                edit = @Edit(title = "员工姓名", notNull = true, search = @Search))
    @Column(name = "employee_name", length = 100, nullable = false)
    private String employeeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "task_id")
    @EruptField(views = @View(title = "任务", column = "subject"),
                edit = @Edit(title = "任务", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "subject")))
    private ProjTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "project_id")
    @EruptField(views = @View(title = "项目", column = "name"),
                edit = @Edit(title = "项目", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ProjProject project;

    @EruptField(views = @View(title = "开始时间"),
                edit = @Edit(title = "开始时间", notNull = true, type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "from_time", nullable = false)
    private LocalDateTime fromTime;

    @EruptField(views = @View(title = "结束时间"),
                edit = @Edit(title = "结束时间", notNull = true, type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "to_time", nullable = false)
    private LocalDateTime toTime;

    /** 派生:hours = Duration 小时(审批时计算回写) */
    @EruptField(views = @View(title = "工时(小时)"), edit = @Edit(title = "工时(小时)", show = false))
    @Column(name = "hours", precision = 12, scale = 4)
    private BigDecimal hours = BigDecimal.ZERO;

    @EruptField(views = @View(title = "计费费率"),
                edit = @Edit(title = "计费费率", numberType = @NumberType(min = 0)))
    @Column(name = "billing_rate", precision = 24, scale = 6)
    private BigDecimal billingRate = BigDecimal.ZERO;

    /** 派生:cost = hours * billingRate(审批时回写) */
    @EruptField(views = @View(title = "成本"), edit = @Edit(title = "成本", show = false))
    @Column(name = "cost", precision = 24, scale = 6)
    private BigDecimal cost = BigDecimal.ZERO;

    @EruptField(views = @View(title = "计费状态"),
                edit = @Edit(title = "计费状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "BillingStatus")))
    @Column(name = "billing_status", nullable = false)
    private Integer billingStatus = BillingStatus.UNBILLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends ProjStateDataProxy<ProjTimesheet> {
        @Override protected String stateFieldName() { return "billingStatus"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ BillingStatus.UNBILLED.code, null };
        }
    }
}
