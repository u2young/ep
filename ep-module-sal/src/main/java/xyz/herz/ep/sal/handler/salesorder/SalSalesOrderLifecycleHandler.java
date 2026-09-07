package xyz.herz.ep.sal.handler.salesorder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.sal.entity.salesorder.SalSalesOrder;
import xyz.herz.ep.sal.enums.SalDictEnums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单生命周期行按钮处理器(参考 ERPNext Sales Order 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细 + 回写总金额)
 * / 完成(SUBMITTED→COMPLETED,回写完成时间)
 * / 暂停(SUBMITTED→ON_HOLD)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,终态不可取消)。
 */
@Component
public class SalSalesOrderLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "sal.sco.submit";
    public static final String CODE_COMPLETE = "sal.sco.complete";
    public static final String CODE_HOLD = "sal.sco.hold";
    public static final String CODE_CANCEL = "sal.sco.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof SalSalesOrder doc)) {
                fail++; sb.append("仅支持销售订单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_COMPLETE -> applyComplete(doc);
                    case CODE_HOLD -> applyHold(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("订单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细 + 回写总金额。 */
    private void applySubmit(SalSalesOrder doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != SalesOrderStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        if (doc.getItems() == null || doc.getItems().isEmpty()) {
            throw new IllegalStateException("订单明细不能为空,无法提交");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (var it : doc.getItems()) {
            BigDecimal qty = it.getQty() == null ? BigDecimal.ZERO : it.getQty();
            BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            it.setAmount(qty.multiply(price));
            total = total.add(it.getAmount());
        }
        doc.setTotalAmount(total);
        doc.setSubmittedAt(LocalDateTime.now());
        doc.setStatus(SalesOrderStatus.SUBMITTED.code);
    }

    /** 完成:已提交 → 已完成,回写完成时间。 */
    private void applyComplete(SalSalesOrder doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != SalesOrderStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可完成,当前状态码: " + st);
        }
        doc.setCompletedAt(LocalDateTime.now());
        doc.setStatus(SalesOrderStatus.COMPLETED.code);
    }

    /** 暂停:已提交 → 已暂停。 */
    private void applyHold(SalSalesOrder doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != SalesOrderStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可暂停,当前状态码: " + st);
        }
        doc.setStatus(SalesOrderStatus.ON_HOLD.code);
    }

    /** 取消:草稿/已提交 → 已取消;终态不可重复取消。 */
    private void applyCancel(SalSalesOrder doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == SalesOrderStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        if (st == SalesOrderStatus.COMPLETED.code) {
            throw new IllegalStateException("已完成不可取消");
        }
        doc.setStatus(SalesOrderStatus.CANCELLED.code);
    }
}