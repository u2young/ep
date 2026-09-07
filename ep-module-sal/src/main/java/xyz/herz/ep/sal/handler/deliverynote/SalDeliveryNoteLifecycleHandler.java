package xyz.herz.ep.sal.handler.deliverynote;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.sal.entity.deliverynote.SalDeliveryNote;
import xyz.herz.ep.sal.enums.SalDictEnums.DeliveryNoteStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 送货单生命周期行按钮处理器(参考 ERPNext Delivery Note 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细 + 回写总金额)
 * / 发货(SUBMITTED→DELIVERED,回写总数量/金额)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,终态不可取消)。
 */
@Component
public class SalDeliveryNoteLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "sal.dn.submit";
    public static final String CODE_DELIVER = "sal.dn.deliver";
    public static final String CODE_CANCEL = "sal.dn.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof SalDeliveryNote doc)) {
                fail++; sb.append("仅支持送货单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_DELIVER -> applyDeliver(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("送货单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细 + 回写总金额。 */
    private void applySubmit(SalDeliveryNote doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != DeliveryNoteStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        if (doc.getItems() == null || doc.getItems().isEmpty()) {
            throw new IllegalStateException("送货明细不能为空,无法提交");
        }
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        for (var it : doc.getItems()) {
            BigDecimal qty = it.getQty() == null ? BigDecimal.ZERO : it.getQty();
            BigDecimal price = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            it.setAmount(qty.multiply(price));
            totalQty = totalQty.add(qty);
            totalAmt = totalAmt.add(it.getAmount());
        }
        doc.setTotalQty(totalQty);
        doc.setTotalAmount(totalAmt);
        doc.setStatus(DeliveryNoteStatus.SUBMITTED.code);
    }

    /** 发货:已提交 → 已发货。 */
    private void applyDeliver(SalDeliveryNote doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != DeliveryNoteStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可发货,当前状态码: " + st);
        }
        doc.setStatus(DeliveryNoteStatus.DELIVERED.code);
    }

    /** 取消:草稿/已提交 → 已取消;终态不可重复取消。 */
    private void applyCancel(SalDeliveryNote doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == DeliveryNoteStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        if (st == DeliveryNoteStatus.DELIVERED.code) {
            throw new IllegalStateException("已发货不可取消");
        }
        doc.setStatus(DeliveryNoteStatus.CANCELLED.code);
    }
}