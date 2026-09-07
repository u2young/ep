package xyz.herz.ep.stk.handler.reconciliation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.stk.entity.reconciliation.StkReconciliationItem;
import xyz.herz.ep.stk.entity.reconciliation.StkStockReconciliation;
import xyz.herz.ep.stk.enums.StkDictEnums.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点单生命周期行按钮处理器(参考 ERPNext Stock Reconciliation 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细 + 回写明细 variance + 主单 totalBookQty/totalActualQty/totalVariance/submittedAt)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,已取消不可再取消)。
 *
 * <p>差异计算规则:variance = actualQty - bookQty(正数盘盈,负数盘亏)。
 */
@Component
public class StkReconciliationLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "stk.recon.submit";
    public static final String CODE_CANCEL = "stk.recon.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof StkStockReconciliation doc)) {
                fail++; sb.append("仅支持库存盘点单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("盘点单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细 + 回写明细 variance + 主单合计。 */
    private void applySubmit(StkStockReconciliation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != ReconciliationStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        List<StkReconciliationItem> items = doc.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("盘点明细不能为空,无法提交");
        }
        BigDecimal totalBook = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalVariance = BigDecimal.ZERO;
        for (StkReconciliationItem it : items) {
            if (it.getBookQty() == null || it.getActualQty() == null) {
                throw new IllegalStateException("明细[" + it.getItemCode() + "]账面/实际数量未填,无法提交");
            }
            // variance = actual - book(正盘盈,负盘亏)
            BigDecimal variance = it.getActualQty().subtract(it.getBookQty());
            it.setVariance(variance);
            totalBook = totalBook.add(it.getBookQty());
            totalActual = totalActual.add(it.getActualQty());
            totalVariance = totalVariance.add(variance);
        }
        doc.setTotalBookQty(totalBook);
        doc.setTotalActualQty(totalActual);
        doc.setTotalVariance(totalVariance);
        doc.setSubmittedAt(LocalDateTime.now());
        if (doc.getReconciler() == null || doc.getReconciler().isBlank()) {
            doc.setReconciler("system");
        }
        doc.setStatus(ReconciliationStatus.SUBMITTED.code);
    }

    /** 取消:草稿/已提交 → 已取消;已取消不可再取消。 */
    private void applyCancel(StkStockReconciliation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == ReconciliationStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(ReconciliationStatus.CANCELLED.code);
    }
}
