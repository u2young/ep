package xyz.herz.ep.pay.entity.slip;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pay.core.PayEnumChoiceFetchHandler;
import xyz.herz.ep.pay.core.PayStateDataProxy;
import xyz.herz.ep.pay.handler.slip.PaySlipLifecycleHandler;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructure;
import xyz.herz.ep.pay.enums.PayDictEnums.SlipStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 工资单(参考 ERPNext Salary Slip DocType)。
 * <p>状态机:0草稿/1已提交/2已过账/3已取消。
 * 提交时从工资结构自动生成明细快照并派生 grossPay/deductions/netPay;
 * 过账时生成 GL 凭证(借 工资费用总额 / 贷 实发 + 贷 代扣款)。
 */
@Getter @Setter
@Entity
@Table(name = "pay_salary_slip")
@Erupt(
    name = "工资单",
    power = @Power(importable = true, export = true),
    dataProxy = PaySalarySlip.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = PaySlipLifecycleHandler.CODE_SUBMIT,
            operationHandler = PaySlipLifecycleHandler.class),
        @RowOperation(title = "过账", code = PaySlipLifecycleHandler.CODE_POST,
            operationHandler = PaySlipLifecycleHandler.class),
        @RowOperation(title = "取消", code = PaySlipLifecycleHandler.CODE_CANCEL,
            operationHandler = PaySlipLifecycleHandler.class)
    }
)
public class PaySalarySlip extends MetaModelVo {

    @EruptField(views = @View(title = "工资单号"),
                edit = @Edit(title = "工资单号", notNull = true, search = @Search))
    @Column(name = "slip_no", length = 50, nullable = false)
    private String slipNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "employee_id", nullable = true)
    @EruptField(views = @View(title = "员工", column = "name"),
                edit = @Edit(title = "员工", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.employee.HrEmployee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "structure_id", nullable = true)
    @EruptField(views = @View(title = "工资结构", column = "name"),
                edit = @Edit(title = "工资结构", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private PaySalaryStructure structure;

    @EruptField(views = @View(title = "薪酬月份"),
                edit = @Edit(title = "薪酬月份", notNull = true, desc = "yyyy-MM 格式"))
    @Column(name = "posting_month", length = 7, nullable = false)
    private String postingMonth;

    @EruptField(views = @View(title = "应发合计"),
                edit = @Edit(title = "应发合计", show = false, desc = "提交时派生: 收入项合计"))
    @Column(name = "gross_pay", precision = 18, scale = 2)
    private BigDecimal grossPay;

    @EruptField(views = @View(title = "扣款合计"),
                edit = @Edit(title = "扣款合计", show = false, desc = "提交时派生: 扣款项合计"))
    @Column(name = "deductions", precision = 18, scale = 2)
    private BigDecimal deductions;

    @EruptField(views = @View(title = "实发合计"),
                edit = @Edit(title = "实发合计", show = false, desc = "提交时派生: grossPay - deductions"))
    @Column(name = "net_pay", precision = 18, scale = 2)
    private BigDecimal netPay;

    @EruptField(views = @View(title = "过账日期"),
                edit = @Edit(title = "过账日期"))
    @Column(name = "posting_date")
    private LocalDate postingDate;

    @EruptField(views = @View(title = "凭证ID"),
                edit = @Edit(title = "凭证ID", show = false, desc = "过账后回写 FinJournalEntry.id"))
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PayEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "SlipStatus")))
    @Column(nullable = false)
    private Integer status = SlipStatus.DRAFT.code;

    @OneToMany(mappedBy = "slip", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "工资明细", type = EditType.TAB_TABLE_ADD))
    private List<PaySalarySlipItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends PayStateDataProxy<PaySalarySlip> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
