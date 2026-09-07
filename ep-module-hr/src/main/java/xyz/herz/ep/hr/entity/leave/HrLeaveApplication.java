package xyz.herz.ep.hr.entity.leave;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.hr.core.HrEnumChoiceFetchHandler;
import xyz.herz.ep.hr.core.HrStateDataProxy;
import xyz.herz.ep.hr.handler.leave.HrLeaveLifecycleHandler;
import xyz.herz.ep.hr.enums.HrDictEnums.LeaveStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 请假单(参考 ERPNext Leave Application DocType)。
 * <p>状态机:0草稿/1已申请/2已批准/3已拒绝/4已取消。
 * 申请时派生 totalDays = toDate - fromDate + 1(天)。
 */
@Getter @Setter
@Entity
@Table(name = "hr_leave_application")
@Erupt(
    name = "请假单",
    power = @Power(importable = true, export = true),
    dataProxy = HrLeaveApplication.Proxy.class,
    rowOperation = {
        @RowOperation(title = "申请", code = HrLeaveLifecycleHandler.CODE_APPLY,
            operationHandler = HrLeaveLifecycleHandler.class),
        @RowOperation(title = "批准", code = HrLeaveLifecycleHandler.CODE_APPROVE,
            operationHandler = HrLeaveLifecycleHandler.class),
        @RowOperation(title = "拒绝", code = HrLeaveLifecycleHandler.CODE_REJECT,
            operationHandler = HrLeaveLifecycleHandler.class),
        @RowOperation(title = "取消", code = HrLeaveLifecycleHandler.CODE_CANCEL,
            operationHandler = HrLeaveLifecycleHandler.class)
    }
)
public class HrLeaveApplication extends MetaModelVo {

    @EruptField(views = @View(title = "单号"),
                edit = @Edit(title = "单号", notNull = true, search = @Search))
    @Column(name = "leave_no", length = 50, nullable = false)
    private String leaveNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employee_id", nullable = true)
    @EruptField(views = @View(title = "员工", column = "name"),
                edit = @Edit(title = "员工", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.employee.HrEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "leave_type_id", nullable = true)
    @EruptField(views = @View(title = "请假类型", column = "name"),
                edit = @Edit(title = "请假类型", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.leavetype.HrLeaveType leaveType;

    @EruptField(views = @View(title = "开始日期"),
                edit = @Edit(title = "开始日期", notNull = true))
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @EruptField(views = @View(title = "结束日期"),
                edit = @Edit(title = "结束日期", notNull = true))
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @EruptField(views = @View(title = "请假天数"),
                edit = @Edit(title = "请假天数", show = false, desc = "申请时派生: toDate - fromDate + 1"))
    @Column(name = "total_days", precision = 6, scale = 1)
    private BigDecimal totalDays;

    @EruptField(views = @View(title = "审批人ID"),
                edit = @Edit(title = "审批人ID", desc = "UPMS user id 快照"))
    @Column(name = "approver_id")
    private Long approverId;

    @EruptField(views = @View(title = "审批人姓名"),
                edit = @Edit(title = "审批人姓名"))
    @Column(name = "approver_name", length = 50)
    private String approverName;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "LeaveStatus")))
    @Column(nullable = false)
    private Integer status = LeaveStatus.DRAFT.code;

    @EruptField(views = @View(title = "请假事由"),
                edit = @Edit(title = "请假事由"))
    @Column(length = 500)
    private String reason;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends HrStateDataProxy<HrLeaveApplication> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
