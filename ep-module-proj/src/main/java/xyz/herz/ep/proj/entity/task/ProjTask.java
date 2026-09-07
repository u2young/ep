package xyz.herz.ep.proj.entity.task;

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
import xyz.herz.ep.proj.enums.ProjDictEnums.TaskStatus;
import xyz.herz.ep.proj.handler.task.ProjTaskLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 任务(参考 ERPNext Task DocType,树形拆解)。
 * <p>状态机:NOT_STARTED(0) → IN_PROGRESS(1) → COMPLETED(2) / OVERDUE(3) / CANCELLED(4)。
 * 只有 NOT_STARTED 允许表单直改;其他状态走行按钮(开工/完成)。
 * <p>parentTask 自关联支持多级树;complete 时刷新 parent progress 并回写项目 percentComplete。
 * assignee 用快照(assigneeId+assigneeName)。
 * progress(0-100) 由派工/工时回写或手工在 IN_PROGRESS 状态下录入。
 */
@Getter @Setter
@Entity
@Table(name = "proj_task")
@Erupt(
    name = "项目任务",
    power = @Power(importable = true, export = true),
    dataProxy = ProjTask.Proxy.class,
    rowOperation = {
        @RowOperation(title = "开工", code = ProjTaskLifecycleHandler.CODE_START,
            operationHandler = ProjTaskLifecycleHandler.class,
            operationParam = { ProjTaskLifecycleHandler.CODE_START }),
        @RowOperation(title = "完成", code = ProjTaskLifecycleHandler.CODE_COMPLETE,
            operationHandler = ProjTaskLifecycleHandler.class,
            operationParam = { ProjTaskLifecycleHandler.CODE_COMPLETE })
    }
)
public class ProjTask extends MetaModelVo {

    @EruptField(views = @View(title = "任务标题"),
                edit = @Edit(title = "任务标题", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "project_id")
    @EruptField(views = @View(title = "项目", column = "name"),
                edit = @Edit(title = "项目", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ProjProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_task_id")
    @EruptField(views = @View(title = "父任务", column = "subject"),
                edit = @Edit(title = "父任务", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "subject")))
    private ProjTask parentTask;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "TaskStatus")))
    @Column(nullable = false)
    private Integer status = TaskStatus.NOT_STARTED.code;

    @EruptField(views = @View(title = "优先级"),
                edit = @Edit(title = "优先级", search = @Search,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "TaskPriority")))
    @Column
    private Integer priority = 2;

    @EruptField(views = @View(title = "计划工时(小时)"),
                edit = @Edit(title = "计划工时(小时)", numberType = @NumberType(min = 0)))
    @Column(name = "expected_time", precision = 12, scale = 2)
    private BigDecimal expectedTime;

    /** 派生:实际工时(由 ProjTimesheet 汇总回写) */
    @EruptField(views = @View(title = "实际工时"), edit = @Edit(title = "实际工时", show = false))
    @Column(name = "actual_time", precision = 12, scale = 2)
    private BigDecimal actualTime = BigDecimal.ZERO;

    @EruptField(views = @View(title = "进度%"),
                edit = @Edit(title = "进度%", numberType = @NumberType(min = 0, max = 100)))
    @Column(name = "progress", precision = 6, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    @EruptField(views = @View(title = "经办人ID"), edit = @Edit(title = "经办人ID", show = false))
    @Column(name = "assignee_id")
    private Long assigneeId;

    @EruptField(views = @View(title = "经办人"),
                edit = @Edit(title = "经办人", search = @Search))
    @Column(name = "assignee_name", length = 100)
    private String assigneeName;

    @EruptField(views = @View(title = "开始日期"),
                edit = @Edit(title = "开始日期", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "start_date")
    private LocalDate startDate;

    @EruptField(views = @View(title = "结束日期"),
                edit = @Edit(title = "结束日期", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "end_date")
    private LocalDate endDate;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends ProjStateDataProxy<ProjTask> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            // 草稿态(未开始 0)允许直改;进行中(1)允许改 progress/actualTime
            return new Object[]{ TaskStatus.NOT_STARTED.code, TaskStatus.IN_PROGRESS.code, null };
        }
    }
}
