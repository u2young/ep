package xyz.herz.ep.hr.entity.attendance;

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
import xyz.herz.ep.hr.handler.attendance.HrAttendanceCheckInHandler;
import xyz.herz.ep.hr.enums.HrDictEnums.AttendanceStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤(参考 ERPNext Attendance DocType)。
 * <p>状态机:0缺勤/1出勤/2迟到/3早退/4请假。
 * 签到行按钮写 inTime 并置 PRESENT;签退行按钮写 outTime 并派生 workHours(小时)。
 */
@Getter @Setter
@Entity
@Table(name = "hr_attendance")
@Erupt(
    name = "考勤",
    power = @Power(importable = true, export = true),
    dataProxy = HrAttendance.Proxy.class,
    rowOperation = {
        @RowOperation(title = "签到", code = HrAttendanceCheckInHandler.CODE_CHECK_IN,
            operationHandler = HrAttendanceCheckInHandler.class),
        @RowOperation(title = "签退", code = HrAttendanceCheckInHandler.CODE_CHECK_OUT,
            operationHandler = HrAttendanceCheckInHandler.class)
    }
)
public class HrAttendance extends MetaModelVo {

    @EruptField(views = @View(title = "考勤单号"),
                edit = @Edit(title = "考勤单号", notNull = true, search = @Search))
    @Column(name = "att_no", length = 50, nullable = false)
    private String attNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employee_id", nullable = true)
    @EruptField(views = @View(title = "员工", column = "name"),
                edit = @Edit(title = "员工", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.employee.HrEmployee employee;

    @EruptField(views = @View(title = "考勤日期"),
                edit = @Edit(title = "考勤日期", notNull = true, search = @Search))
    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AttendanceStatus")))
    @Column(nullable = false)
    private Integer status = AttendanceStatus.ABSENT.code;

    @EruptField(views = @View(title = "签到时间"),
                edit = @Edit(title = "签到时间", show = false))
    @Column(name = "in_time")
    private LocalDateTime inTime;

    @EruptField(views = @View(title = "签退时间"),
                edit = @Edit(title = "签退时间", show = false))
    @Column(name = "out_time")
    private LocalDateTime outTime;

    @EruptField(views = @View(title = "工时(小时)"),
                edit = @Edit(title = "工时(小时)", show = false, desc = "签退时派生: (outTime - inTime)/60"))
    @Column(name = "work_hours", precision = 6, scale = 2)
    private BigDecimal workHours;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends HrStateDataProxy<HrAttendance> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
