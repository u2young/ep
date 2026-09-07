package xyz.herz.ep.fin.entity.invoice;

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
import xyz.herz.ep.fin.enums.FinDictEnums.InvoiceStatus;
import xyz.herz.ep.fin.handler.invoice.FinSalesInvoiceSubmitHandler;
import xyz.herz.ep.fin.handler.invoice.FinSalesInvoiceCancelHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售(应收)发票(参考 ERPNext Sales Invoice DocType)。
 * <p>状态机:DRAFT(0) → SUBMITTED(1,触发 GL 借应收/贷收入) → PAID(2) / PARTIAL_PAID(3) / CANCELLED(4)。
 * <p>客户字段用 customerId + customerName 快照(不 REF 跨模块 ErpCustomer,保持 fin 独立可测)。
 */
@Getter @Setter
@Entity
@Table(name = "fin_sales_invoice")
@Erupt(
    name = "销售发票",
    power = @Power(importable = true, export = true),
    dataProxy = FinSalesInvoice.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = FinSalesInvoiceSubmitHandler.CODE_SUBMIT, icon = "fa fa-check-circle",
            operationHandler = FinSalesInvoiceSubmitHandler.class, operationParam = { FinSalesInvoiceSubmitHandler.CODE_SUBMIT }),
        @RowOperation(title = "取消", code = FinSalesInvoiceCancelHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = FinSalesInvoiceCancelHandler.class, operationParam = { FinSalesInvoiceCancelHandler.CODE_CANCEL })
    }
)
public class FinSalesInvoice extends MetaModelVo {

    @EruptField(views = @View(title = "发票号"),
                edit = @Edit(title = "发票号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "入账日期"),
                edit = @Edit(title = "入账日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(nullable = false)
    private LocalDate postingDate = LocalDate.now();

    @EruptField(views = @View(title = "到期日"),
                edit = @Edit(title = "到期日", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(nullable = false)
    private LocalDate dueDate = LocalDate.now().plusDays(30);

    @EruptField(views = @View(title = "客户 ID"), edit = @Edit(title = "客户 ID", show = false))
    @Column(name = "customer_id")
    private Long customerId;

    @EruptField(views = @View(title = "客户名称"),
                edit = @Edit(title = "客户名称", notNull = true))
    @Column(name = "customer_name", length = 200)
    private String customerName;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "InvoiceStatus")))
    @Column(nullable = false)
    private Integer status = InvoiceStatus.DRAFT.code;

    @EruptField(views = @View(title = "发票金额"), edit = @Edit(title = "发票金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal total = BigDecimal.ZERO;

    @EruptField(views = @View(title = "价税合计"), edit = @Edit(title = "价税合计", show = false))
    @Column(name = "grand_total", precision = 24, scale = 6)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已付金额"), edit = @Edit(title = "已付金额", show = false))
    @Column(name = "paid_amount", precision = 24, scale = 6)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "未付金额"), edit = @Edit(title = "未付金额", show = false))
    @Column(name = "outstanding_amount", precision = 24, scale = 6)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "来源类型"), edit = @Edit(title = "来源类型", show = false))
    @Column(length = 40)
    private String sourceType;

    @EruptField(views = @View(title = "来源 ID"), edit = @Edit(title = "来源 ID", show = false))
    @Column(name = "source_id")
    private Long sourceId;

    @EruptField(views = @View(title = "来源单号"), edit = @Edit(title = "来源单号", show = false))
    @Column(length = 60)
    private String sourceNo;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends FinStateDataProxy<FinSalesInvoice> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ InvoiceStatus.DRAFT.code, null };
        }
    }
}
