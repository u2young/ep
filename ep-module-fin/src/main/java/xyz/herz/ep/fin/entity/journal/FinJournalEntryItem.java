package xyz.herz.ep.fin.entity.journal;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.fin.entity.account.FinAccount;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 凭证明细行(参考 ERPNext Journal Entry Account 子表)。
 * <p>extends {@link BaseModel} 明细子表,自带 id/createTime/updateTime。
 * <p>记账规则:debit 与 credit 必须互斥(一个 >0 时另一个 == 0),由 FinPostingService 校验。
 */
@Getter @Setter
@Entity
@Table(name = "fin_journal_entry_item")
public class FinJournalEntryItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "科目", column = "name"),
                edit = @Edit(title = "科目", notNull = true))
    private FinAccount account;

    @EruptField(views = @View(title = "借方"), edit = @Edit(title = "借方", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal debit = BigDecimal.ZERO;

    @EruptField(views = @View(title = "贷方"), edit = @Edit(title = "贷方", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal credit = BigDecimal.ZERO;

    @EruptField(views = @View(title = "往来类型"), edit = @Edit(title = "往来类型"))
    @Column(length = 20)
    private String partyType;

    @EruptField(views = @View(title = "往来 ID"), edit = @Edit(title = "往来 ID", show = false))
    @Column(name = "party_id")
    private Long partyId;

    @EruptField(views = @View(title = "往来名称"), edit = @Edit(title = "往来名称"))
    @Column(length = 200)
    private String partyName;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
