package xyz.herz.ep.proj.entity.activitycost;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.proj.core.ProjEnumChoiceFetchHandler;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.enums.ProjDictEnums.ActivityCostType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目活动成本(参考 ERPNext Activity Cost)。
 * <p>costType: LABOR(1人工)/EXPENSE(2费用)/MATERIAL(3物料);每行一笔成本归集。
 * <p>由 Timesheet.approve(costType=LABOR)和 ExpenseClaim.post(costType=EXPENSE)插入;
 * sourceType/sourceId 记录来源单据,支持回溯。
 * 项目 totalCost = sum(ProjActivityCost.amount where project_id=X)。
 */
@Getter @Setter
@Entity
@Table(name = "proj_activity_cost")
@Erupt(name = "项目活动成本", power = @Power(importable = true, export = true))
public class ProjActivityCost extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "project_id")
    @EruptField(views = @View(title = "项目", column = "name"),
                edit = @Edit(title = "项目", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ProjProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "task_id")
    @EruptField(views = @View(title = "任务", column = "subject"),
                edit = @Edit(title = "任务", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "subject")))
    private ProjTask task;

    @EruptField(views = @View(title = "成本类型"),
                edit = @Edit(title = "成本类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ActivityCostType")))
    @Column(name = "cost_type", nullable = false)
    private Integer costType = ActivityCostType.LABOR.code;

    @EruptField(views = @View(title = "金额"),
                edit = @Edit(title = "金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "amount", precision = 24, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "入账日期"),
                edit = @Edit(title = "入账日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @EruptField(views = @View(title = "来源类型"),
                edit = @Edit(title = "来源类型"))
    @Column(name = "source_type", length = 60)
    private String sourceType;

    @EruptField(views = @View(title = "来源ID"), edit = @Edit(title = "来源ID", show = false))
    @Column(name = "source_id")
    private Long sourceId;
}
