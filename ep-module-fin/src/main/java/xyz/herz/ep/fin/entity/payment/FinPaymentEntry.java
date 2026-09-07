package xyz.herz.ep.fin.entity.payment;

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
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentType;
import xyz.herz.ep.fin.handler.payment.FinPaymentSubmitHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收付款单(三合一,参考 ERPNext Payment Entry DocType)。
 * <p>状态机:DRAFT(0) → SUBMITTED(1,触发 GL 借/贷方向看 paymentType) / CANCELLED(2)。
 * <p>支持三种类型:1 收款(RECEIVE)/2 付款(PAY)/3 内部转账(INTERNAL_TRANSFER)。
 * <p>往来方用 partyType + partyId + partyName 快照(不 REF 跨模块,可代表 Customer/Supplier/Employee)。
 * <p>与既有 ErpFinancePayment/Receipt 共存:本类是合并超集,通过 FinLegacyFinanceBridge 可选桥接。
 */
@Getter @Setter
@Entity
@Table(name = "fin_payment_entry")
@Erupt(
    name = "收付款单",
    power = @Power(importable = true, export = true),
    dataProxy = FinPaymentEntry.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = FinPaymentSubmitHandler.CODE_SUBMIT, icon = "fa fa-check-circle",
            operationHandler = FinPaymentSubmitHandler.class, operationParam = { FinPaymentSubmitHandler.CODE_SUBMIT })
    }
)
public class FinPaymentEntry extends MetaModelVo {

    @EruptField(views = @View(title = "单号"),
                edit = @Edit(title = "单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "入账日期"),
                edit = @Edit(title = "入账日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(nullable = false)
    private LocalDate postingDate = LocalDate.now();

    @EruptField(views = @View(title = "收付类型"),
                edit = @Edit(title = "收付类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PaymentType")))
    @Column(name = "payment_type", nullable = false)
    private Integer paymentType = PaymentType.RECEIVE.code;

    @EruptField(views = @View(title = "往来类型"),
                edit = @Edit(title = "往来类型", notNull = true,
                    desc = "Customer/Supplier/Employee"))
    @Column(name = "party_type", length = 20, nullable = false)
    private String partyType = "Customer";

    @EruptField(views = @View(title = "往来 ID"), edit = @Edit(title = "往来 ID", show = false))
    @Column(name = "party_id")
    private Long partyId;

    @EruptField(views = @View(title = "往来名称"),
                edit = @Edit(title = "往来名称", notNull = true))
    @Column(name = "party_name", length = 200)
    private String partyName;

    @EruptField(views = @View(title = "收付金额"),
                edit = @Edit(title = "收付金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "paid_amount", nullable = false, precision = 24, scale = 6)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "银行流水号"), edit = @Edit(title = "银行流水号"))
    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @EruptField(views = @View(title = "生成凭证 ID"), edit = @Edit(title = "生成凭证 ID", show = false))
    @Column(name = "generated_journal_id")
    private Long generatedJournalId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PaymentStatus")))
    @Column(nullable = false)
    private Integer status = PaymentStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends FinStateDataProxy<FinPaymentEntry> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ PaymentStatus.DRAFT.code, null };
        }
    }
}
