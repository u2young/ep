package xyz.herz.ep.sal.handler.quotation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.sal.entity.quotation.SalQuotation;
import xyz.herz.ep.sal.enums.SalDictEnums.QuotationStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 报价单生命周期行按钮处理器(参考 ERPNext Quotation 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细存在 + 回写 expectedAmount)
 * / 拒绝(SUBMITTED→REJECTED)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,终态不可取消)。
 * <p>报价接受(SUBMITTED→ACCEPTED):客户接受后可转销售订单。
 */
@Component
public class SalQuotationLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "sal.qtn.submit";
    public static final String CODE_REJECT = "sal.qtn.reject";
    public static final String CODE_CANCEL = "sal.qtn.cancel";
    public static final String CODE_ACCEPT = "sal.qtn.accept";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof SalQuotation doc)) {
                fail++; sb.append("仅支持报价单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_REJECT -> applyReject(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    case CODE_ACCEPT -> applyAccept(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("报价单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细存在 + 回写预计金额。 */
    private void applySubmit(SalQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != QuotationStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        if (doc.getItems() == null || doc.getItems().isEmpty()) {
            throw new IllegalStateException("报价明细不能为空,无法提交");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (var it : doc.getItems()) {
            BigDecimal qty = it.getQty() == null ? BigDecimal.ZERO : it.getQty();
            BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            it.setAmount(qty.multiply(price));
            total = total.add(it.getAmount());
        }
        doc.setExpectedAmount(total);
        doc.setStatus(QuotationStatus.SUBMITTED.code);
    }

    /** 拒绝:已提交 → 已拒绝。 */
    private void applyReject(SalQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != QuotationStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可拒绝,当前状态码: " + st);
        }
        doc.setStatus(QuotationStatus.REJECTED.code);
    }

    /** 取消:草稿/已提交 → 已取消;终态不可重复取消。 */
    private void applyCancel(SalQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == QuotationStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        if (st == QuotationStatus.ACCEPTED.code) {
            throw new IllegalStateException("已接受不可取消");
        }
        doc.setStatus(QuotationStatus.CANCELLED.code);
    }

    /** 接受:已提交 → 已接受(可转销售订单)。 */
    private void applyAccept(SalQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != QuotationStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可接受,当前状态码: " + st);
        }
        doc.setStatus(QuotationStatus.ACCEPTED.code);
    }
}