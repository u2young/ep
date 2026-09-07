package xyz.herz.ep.pay.handler.slip;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;
import xyz.herz.ep.pay.entity.slip.PaySalarySlipItem;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructure;
import xyz.herz.ep.pay.enums.PayDictEnums.ComponentType;
import xyz.herz.ep.pay.enums.PayDictEnums.SlipStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 工资单生命周期行按钮处理器(参考 ERPNext Salary Slip 工作流)。
 * <p>提交(DRAFT→SUBMITTED,从工资结构自动生成明细快照 + 派生 gross/deductions/net)
 * / 过账(SUBMITTED→POSTED,GL 借 工资费用总额 / 贷 实发 + 贷 代扣款)
 * / 取消(非 CANCELLED→CANCELLED,已过账的生成反向冲销凭证)。
 *
 * <p>GL 分录(三行,借贷平衡):
 * <pre>
 * 借 SALARY_EXP   grossPay  (工资费用总额)
 * 贷 SALARY_PAY   netPay    (实发工资)
 * 贷 SALARY_WITHHOLD deductions (代扣社保/公积金/个税)
 * </pre>
 */
@Component
public class PaySlipLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "pay.slip.submit";
    public static final String CODE_POST = "pay.slip.post";
    public static final String CODE_CANCEL = "pay.slip.cancel";

    /** GL 科目码(测试/生产需在 fin_account 预置同名科目)。 */
    public static final String ACC_SALARY_EXPENSE = "SALARY_EXP";
    public static final String ACC_SALARY_PAYABLE = "SALARY_PAY";
    public static final String ACC_SALARY_WITHHOLDING = "SALARY_WITHHOLD";

    @PersistenceContext
    private EntityManager em;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof PaySalarySlip doc)) {
                fail++; sb.append("仅支持工资单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_POST -> applyPost(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工资单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,从工资结构自动生成明细快照 + 派生 gross/deductions/net。 */
    private void applySubmit(PaySalarySlip doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != SlipStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        PaySalaryStructure structure = doc.getStructure();
        if (structure == null) {
            throw new IllegalStateException("工资结构不能为空,无法生成明细");
        }
        if (structure.getItems() == null || structure.getItems().isEmpty()) {
            throw new IllegalStateException("工资结构[" + structure.getName() + "]无明细,无法生成工资单");
        }
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal ded = BigDecimal.ZERO;
        List<PaySalarySlipItem> snapshot = new ArrayList<>();
        for (var si : structure.getItems()) {
            if (si.getComponent() == null) {
                throw new IllegalStateException("结构明细缺少组件定义");
            }
            PaySalarySlipItem item = new PaySalarySlipItem();
            item.setSlip(doc);
            item.setComponentCode(si.getComponent().getCode());
            item.setComponentName(si.getComponent().getName());
            item.setComponentType(si.getComponent().getComponentType());
            item.setAmount(si.getAmount() == null ? BigDecimal.ZERO : si.getAmount());
            snapshot.add(item);
            if (si.getComponent().getComponentType() == ComponentType.EARNING.code) {
                gross = gross.add(item.getAmount());
            } else if (si.getComponent().getComponentType() == ComponentType.DEDUCTION.code) {
                ded = ded.add(item.getAmount());
            }
        }
        doc.getItems().clear();
        doc.getItems().addAll(snapshot);
        doc.setGrossPay(gross);
        doc.setDeductions(ded);
        doc.setNetPay(gross.subtract(ded));
        doc.setStatus(SlipStatus.SUBMITTED.code);
    }

    /** 过账:已提交 → 已过账,GL 借 费用总额 / 贷 实发 + 贷 代扣款。 */
    private void applyPost(PaySalarySlip doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != SlipStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可过账,当前状态码: " + st);
        }
        if (postingFacade != null) {
            Long employeeId = doc.getEmployee() != null ? doc.getEmployee().getId() : null;
            String employeeName = doc.getEmployee() != null ? doc.getEmployee().getName() : null;
            List<FinPostingFacade.PostingLine> lines = new ArrayList<>();
            if (doc.getGrossPay().signum() > 0) {
                lines.add(new FinPostingFacade.PostingLine(ACC_SALARY_EXPENSE,
                    doc.getGrossPay(), null, "Employee", employeeId, employeeName,
                    null, "工资费用总额"));
            }
            if (doc.getNetPay().signum() > 0) {
                lines.add(new FinPostingFacade.PostingLine(ACC_SALARY_PAYABLE,
                    null, doc.getNetPay(), "Employee", employeeId, employeeName,
                    null, "实发工资"));
            }
            if (doc.getDeductions().signum() > 0) {
                lines.add(new FinPostingFacade.PostingLine(ACC_SALARY_WITHHOLDING,
                    null, doc.getDeductions(), "Employee", employeeId, employeeName,
                    null, "代扣社保/公积金/个税"));
            }
            Long journalId = postingFacade.post(new FinPostingFacade.PostingRequest(
                JournalSourceType.SALARY.code, doc.getId(), doc.getSlipNo(),
                doc.getPostingDate() != null ? doc.getPostingDate() : LocalDate.now(),
                lines, "工资过账 " + doc.getPostingMonth()));
            doc.setJournalEntryId(journalId);
        }
        doc.setPostingDate(LocalDate.now());
        doc.setStatus(SlipStatus.POSTED.code);
    }

    /** 取消:非已取消 → 已取消;已过账的生成反向冲销凭证。 */
    private void applyCancel(PaySalarySlip doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == SlipStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        if (st == SlipStatus.POSTED.code && postingFacade != null) {
            postingFacade.cancel(JournalSourceType.SALARY.code, doc.getId());
            doc.setJournalEntryId(null);
        }
        doc.setStatus(SlipStatus.CANCELLED.code);
    }
}
