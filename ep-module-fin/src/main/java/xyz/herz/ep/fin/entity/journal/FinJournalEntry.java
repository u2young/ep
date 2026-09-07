package xyz.herz.ep.fin.entity.journal;

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
import xyz.herz.ep.fin.enums.FinDictEnums.JournalStatus;
import xyz.herz.ep.fin.handler.journal.FinJournalCancelHandler;
import xyz.herz.ep.fin.handler.journal.FinJournalSubmitHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 总账凭证(参考 ERPNext Journal Entry DocType)。
 * <p>状态机:DRAFT(0) → SUBMITTED(1,通过 Submit 按钮) / CANCELLED(2,通过 Cancel 按钮)。
 * <p>平账校验:totalDebit == totalCredit(由 FinPostingService 在 submit 时回写)。
 */
@Getter @Setter
@Entity
@Table(name = "fin_journal_entry")
@Erupt(
    name = "总账凭证",
    power = @Power(importable = true, export = true),
    dataProxy = FinJournalEntry.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = FinJournalSubmitHandler.CODE_SUBMIT, icon = "fa fa-check-circle",
            operationHandler = FinJournalSubmitHandler.class, operationParam = { FinJournalSubmitHandler.CODE_SUBMIT }),
        @RowOperation(title = "取消", code = FinJournalCancelHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = FinJournalCancelHandler.class, operationParam = { FinJournalCancelHandler.CODE_CANCEL })
    }
)
public class FinJournalEntry extends MetaModelVo {

    @EruptField(views = @View(title = "凭证号"),
                edit = @Edit(title = "凭证号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "入账日期"),
                edit = @Edit(title = "入账日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(nullable = false)
    private LocalDate postingDate = LocalDate.now();

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "JournalStatus")))
    @Column(nullable = false)
    private Integer status = JournalStatus.DRAFT.code;

    @EruptField(views = @View(title = "借方合计"), edit = @Edit(title = "借方合计", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @EruptField(views = @View(title = "贷方合计"), edit = @Edit(title = "贷方合计", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @EruptField(views = @View(title = "来源类型"), edit = @Edit(title = "来源类型", show = false))
    @Column(length = 40)
    private String sourceType;

    @EruptField(views = @View(title = "来源单号"), edit = @Edit(title = "来源单号", show = false))
    @Column(length = 60)
    private String sourceNo;

    @EruptField(views = @View(title = "来源 ID"), edit = @Edit(title = "来源 ID", show = false))
    @Column(name = "source_id")
    private Long sourceId;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "journal_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "凭证明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<FinJournalEntryItem> items = new ArrayList<>();

    public static class Proxy extends FinStateDataProxy<FinJournalEntry> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ JournalStatus.DRAFT.code, null };
        }
    }
}
