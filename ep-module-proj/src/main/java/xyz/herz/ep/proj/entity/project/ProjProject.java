package xyz.herz.ep.proj.entity.project;

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
import xyz.herz.ep.proj.enums.ProjDictEnums.ProjectStatus;
import xyz.herz.ep.proj.handler.project.ProjProjectLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目(参考 ERPNext Project DocType)。
 * <p>状态机:DRAFT(0) → APPROVED(1) → IN_PROGRESS(2) → COMPLETED(4) / ON_HOLD(3) / CANCELLED(5)。
 * 只有 DRAFT 允许表单直改;其他状态走行按钮(立项/开工/完工/暂停/取消)。
 * <p>客户/成本中心用快照(customerId+customerName/costCenterId),不 REF 跨模块实体,保持独立可测。
 * <p>percentComplete/totalCost 为派生字段(由任务/工时/费用回写)。
 * 完工时触发收入确认 GL(I-10):借应收/贷收入,通过 FinPostingFacade。
 */
@Getter @Setter
@Entity
@Table(name = "proj_project")
@Erupt(
    name = "项目",
    power = @Power(importable = true, export = true),
    dataProxy = ProjProject.Proxy.class,
    rowOperation = {
        @RowOperation(title = "立项", code = ProjProjectLifecycleHandler.CODE_APPROVE,
            operationHandler = ProjProjectLifecycleHandler.class,
            operationParam = { ProjProjectLifecycleHandler.CODE_APPROVE }),
        @RowOperation(title = "开工", code = ProjProjectLifecycleHandler.CODE_START,
            operationHandler = ProjProjectLifecycleHandler.class,
            operationParam = { ProjProjectLifecycleHandler.CODE_START }),
        @RowOperation(title = "完工", code = ProjProjectLifecycleHandler.CODE_COMPLETE,
            operationHandler = ProjProjectLifecycleHandler.class,
            operationParam = { ProjProjectLifecycleHandler.CODE_COMPLETE }),
        @RowOperation(title = "暂停", code = ProjProjectLifecycleHandler.CODE_PAUSE,
            operationHandler = ProjProjectLifecycleHandler.class,
            operationParam = { ProjProjectLifecycleHandler.CODE_PAUSE }),
        @RowOperation(title = "取消", code = ProjProjectLifecycleHandler.CODE_CANCEL,
            operationHandler = ProjProjectLifecycleHandler.class,
            operationParam = { ProjProjectLifecycleHandler.CODE_CANCEL })
    }
)
public class ProjProject extends MetaModelVo {

    @EruptField(views = @View(title = "项目编号"),
                edit = @Edit(title = "项目编号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "项目名称"),
                edit = @Edit(title = "项目名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "项目类型"),
                edit = @Edit(title = "项目类型", search = @Search))
    @Column(length = 60)
    private String projectType;

    @EruptField(views = @View(title = "客户ID"), edit = @Edit(title = "客户ID", show = false))
    @Column(name = "customer_id")
    private Long customerId;

    @EruptField(views = @View(title = "客户名称"),
                edit = @Edit(title = "客户名称", search = @Search))
    @Column(name = "customer_name", length = 200)
    private String customerName;

    @EruptField(views = @View(title = "计划开始"),
                edit = @Edit(title = "计划开始", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "expected_start")
    private LocalDate expectedStart;

    @EruptField(views = @View(title = "计划结束"),
                edit = @Edit(title = "计划结束", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "expected_end")
    private LocalDate expectedEnd;

    @EruptField(views = @View(title = "实际开始"), edit = @Edit(title = "实际开始", show = false))
    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @EruptField(views = @View(title = "实际完工"), edit = @Edit(title = "实际完工", show = false))
    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    /** 派生:任务完工率(0-100),由 ProjTaskLifecycleHandler 完成时刷新 */
    @EruptField(views = @View(title = "完工%"), edit = @Edit(title = "完工%", show = false))
    @Column(name = "percent_complete", precision = 6, scale = 2)
    private BigDecimal percentComplete = BigDecimal.ZERO;

    /** 派生:累计成本(工时+费用+物料),由 ActivityCost/Timesheet/Expense 回写 */
    @EruptField(views = @View(title = "累计成本"), edit = @Edit(title = "累计成本", show = false))
    @Column(name = "total_cost", precision = 24, scale = 6)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @EruptField(views = @View(title = "预计收入"),
                edit = @Edit(title = "预计收入", numberType = @NumberType(min = 0)))
    @Column(name = "total_revenue", precision = 24, scale = 6)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @EruptField(views = @View(title = "毛利"), edit = @Edit(title = "毛利", show = false))
    @Column(name = "gross_margin", precision = 24, scale = 6)
    private BigDecimal grossMargin = BigDecimal.ZERO;

    @EruptField(views = @View(title = "成本中心ID"), edit = @Edit(title = "成本中心ID", show = false))
    @Column(name = "cost_center_id")
    private Long costCenterId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ProjectStatus")))
    @Column(nullable = false)
    private Integer status = ProjectStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends ProjStateDataProxy<ProjProject> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ ProjectStatus.DRAFT.code, null };
        }
    }
}
