package xyz.herz.ep.erp.entity.finance;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.core.ErpStateDataProxy;
import xyz.herz.ep.erp.entity.master.ErpAccount;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.erp.enums.ErpDictEnums.FinanceDocStatus;
import xyz.herz.ep.erp.handler.finance.ErpPaymentConfirmHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款单(应付)。
 * <p>状态机:DRAFT(0) → CONFIRMED(1,通过行按钮确认)。
 * <p>totalAmount = 付款总金额;paidAmount = 已核销金额(由 ErpFinanceSettlement 写回,只读)。
 */
@Getter @Setter
@Entity
@Table(name = "erp_finance_payment")
@Erupt(
    name = "付款单",
    power = @Power(importable = true, export = true),
    dataProxy = ErpFinancePayment.Proxy.class,
    rowOperation = {
        @RowOperation(title = "确认", code = ErpPaymentConfirmHandler.CODE_CONFIRM, icon = "fa fa-check-circle",
            operationHandler = ErpPaymentConfirmHandler.class, operationParam = { ErpPaymentConfirmHandler.CODE_CONFIRM })
    }
)
public class ErpFinancePayment extends MetaModelVo {

    @EruptField(views = @View(title = "付款单号"),
                edit = @Edit(title = "付款单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "FinanceDocStatus")))
    private Integer status = FinanceDocStatus.DRAFT.code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    @EruptField(views = @View(title = "供应商", column = "name"),
                edit = @Edit(title = "供应商", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpSupplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @EruptField(views = @View(title = "结算账户", column = "name"),
                edit = @Edit(title = "结算账户", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpAccount account;

    @EruptField(views = @View(title = "付款总金额"),
                edit = @Edit(title = "付款总金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已核销金额"),
                edit = @Edit(title = "已核销金额", show = false, desc = "由核销记录回写,只读"))
    @Column(precision = 24, scale = 6)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "付款日期"),
                edit = @Edit(title = "付款日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(nullable = false)
    private LocalDate paymentDate = LocalDate.now();

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends ErpStateDataProxy<ErpFinancePayment> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ FinanceDocStatus.DRAFT.code, null };
        }
    }
}
