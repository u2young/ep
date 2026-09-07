package xyz.herz.ep.proj.entity.expense;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.MetaModelVo;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 费用报销明细行(参考 ERPNext Expense Claim Detail)。
 * <p>每行:费用说明 + 金额;claim 汇总 totalAmount = sum(item.amount)。
 */
@Getter @Setter
@Entity
@Table(name = "proj_expense_claim_item")
public class ProjExpenseClaimItem extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    @EruptField(views = @View(title = "报销单"))
    private ProjExpenseClaim claim;

    @EruptField(views = @View(title = "费用说明"),
                edit = @Edit(title = "费用说明", notNull = true))
    @Column(name = "description", length = 200, nullable = false)
    private String description;

    @EruptField(views = @View(title = "金额"),
                edit = @Edit(title = "金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "amount", precision = 24, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;
}
