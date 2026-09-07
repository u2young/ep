package xyz.herz.ep.fin.entity.budget;

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
import xyz.herz.ep.fin.core.FinEnumChoiceFetchHandler;
import xyz.herz.ep.fin.core.FinStateDataProxy;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;
import xyz.herz.ep.fin.enums.FinDictEnums.BudgetStatus;
import xyz.herz.ep.fin.handler.budget.FinBudgetApproveHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 预算(参考 ERPNext Budget DocType)。
 * <p>状态机:DRAFT(0) → APPROVED(1,通过 Approve 按钮) → CLOSED(2)。
 * <p>字段 fiscalYear 规避 H2 关键字 year(列名 fiscal_year)。
 */
@Getter @Setter
@Entity
@Table(name = "fin_budget")
@Erupt(
    name = "预算",
    power = @Power(importable = true, export = true),
    dataProxy = FinBudget.Proxy.class,
    rowOperation = {
        @RowOperation(title = "批准", code = FinBudgetApproveHandler.CODE_APPROVE, icon = "fa fa-check-circle",
            operationHandler = FinBudgetApproveHandler.class, operationParam = { FinBudgetApproveHandler.CODE_APPROVE })
    }
)
public class FinBudget extends MetaModelVo {

    @EruptField(views = @View(title = "成本中心", column = "name"),
                edit = @Edit(title = "成本中心", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private FinCostCenter costCenter;

    @EruptField(views = @View(title = "会计年度"),
                edit = @Edit(title = "会计年度", notNull = true, search = @Search))
    @Column(name = "fiscal_year", length = 10, nullable = false)
    private String fiscalYear = "2026";

    @EruptField(views = @View(title = "预算金额"),
                edit = @Edit(title = "预算金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "budget_amount", nullable = false, precision = 24, scale = 6)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已分配金额"), edit = @Edit(title = "已分配金额", show = false))
    @Column(name = "allocated_amount", precision = 24, scale = 6)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "实际金额"), edit = @Edit(title = "实际金额", show = false))
    @Column(name = "actual_amount", precision = 24, scale = 6)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "BudgetStatus")))
    @Column(nullable = false)
    private Integer status = BudgetStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends FinStateDataProxy<FinBudget> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ BudgetStatus.DRAFT.code, null };
        }
    }
}
