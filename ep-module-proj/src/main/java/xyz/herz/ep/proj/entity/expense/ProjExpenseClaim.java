package xyz.herz.ep.proj.entity.expense;

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
import xyz.herz.ep.proj.enums.ProjDictEnums.ExpenseClaimStatus;
import xyz.herz.ep.proj.handler.expense.ProjExpenseClaimLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用报销单(参考 ERPNext Expense Claim DocType)。
 * <p>状态机:DRAFT(0) → SUBMITTED(1) → APPROVED(2) / REJECTED(3);APPROVED → POSTED(4)。
 * 只有 DRAFT 允许表单直改;其他走行按钮(提交/批准/拒绝/入账)。
 * <p>Post(I-9)时:借项目成本科目/贷应付薪酬科目,通过 FinPostingFacade 触发 GL;
 * 同时累加 ProjActivityCost(costType=EXPENSE) + ProjProject.totalCost。
 * totalAmount = sum(items.amount);sanctionedAmount 由批准时回写(默认 = totalAmount)。
 * 员工用快照(employeeId+employeeName)。
 */
@Getter @Setter
@Entity
@Table(name = "proj_expense_claim")
@Erupt(
    name = "项目费用报销",
    power = @Power(importable = true, export = true),
    dataProxy = ProjExpenseClaim.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = ProjExpenseClaimLifecycleHandler.CODE_SUBMIT,
            operationHandler = ProjExpenseClaimLifecycleHandler.class,
            operationParam = { ProjExpenseClaimLifecycleHandler.CODE_SUBMIT }),
        @RowOperation(title = "批准", code = ProjExpenseClaimLifecycleHandler.CODE_APPROVE,
            operationHandler = ProjExpenseClaimLifecycleHandler.class,
            operationParam = { ProjExpenseClaimLifecycleHandler.CODE_APPROVE }),
        @RowOperation(title = "拒绝", code = ProjExpenseClaimLifecycleHandler.CODE_REJECT,
            operationHandler = ProjExpenseClaimLifecycleHandler.class,
            operationParam = { ProjExpenseClaimLifecycleHandler.CODE_REJECT }),
        @RowOperation(title = "入账", code = ProjExpenseClaimLifecycleHandler.CODE_POST,
            operationHandler = ProjExpenseClaimLifecycleHandler.class,
            operationParam = { ProjExpenseClaimLifecycleHandler.CODE_POST })
    }
)
public class ProjExpenseClaim extends MetaModelVo {

    @EruptField(views = @View(title = "报销单号"),
                edit = @Edit(title = "报销单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "员工ID"), edit = @Edit(title = "员工ID", show = false))
    @Column(name = "employee_id")
    private Long employeeId;

    @EruptField(views = @View(title = "员工姓名"),
                edit = @Edit(title = "员工姓名", notNull = true, search = @Search))
    @Column(name = "employee_name", length = 100, nullable = false)
    private String employeeName;

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

    /** 派生:sum(items.amount),提交/批准时回写 */
    @EruptField(views = @View(title = "报销总额"), edit = @Edit(title = "报销总额", show = false))
    @Column(name = "total_amount", precision = 24, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 批准时回写的核准金额(默认 = totalAmount) */
    @EruptField(views = @View(title = "核准金额"), edit = @Edit(title = "核准金额", show = false))
    @Column(name = "sanctioned_amount", precision = 24, scale = 6)
    private BigDecimal sanctionedAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ExpenseClaimStatus")))
    @Column(nullable = false)
    private Integer status = ExpenseClaimStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "明细行", type = EditType.TAB_TABLE_ADD))
    private List<ProjExpenseClaimItem> items = new ArrayList<>();

    public static class Proxy extends ProjStateDataProxy<ProjExpenseClaim> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ ExpenseClaimStatus.DRAFT.code, null };
        }
    }
}
