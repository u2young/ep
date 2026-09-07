package xyz.herz.ep.pur.handler.receipt;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;
import xyz.herz.ep.pur.entity.receipt.PurReceiptItem;
import xyz.herz.ep.pur.enums.PurDictEnums.ReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购收货单生命周期行按钮处理器(参考 ERPNext Purchase Receipt 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细存在 + 回写明细 amount = receivedQty × unitPrice)
 * / 收货(SUBMITTED→RECEIVED,校验实收数量已填 + 回写主单 receivedQty/receivedAmount/receivedAt/receiver)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,已收货终态不可取消)。
 *
 * <p>收货语义:实收数量(receivedQty)由收货员在明细行填写;收货时回写主单合计。
 * 与既有 ep-module-erp 的 ErpPurchaseOrder 共存,本单覆盖 ERPNext 采购专属收货流程。
 */
@Component
public class PurReceiptLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "pur.rcp.submit";
    public static final String CODE_RECEIVE = "pur.rcp.receive";
    public static final String CODE_CANCEL = "pur.rcp.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof PurPurchaseReceipt doc)) {
                fail++; sb.append("仅支持采购收货单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_RECEIVE -> applyReceive(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("收货单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细存在 + 回写明细 amount = receivedQty × unitPrice。 */
    private void applySubmit(PurPurchaseReceipt doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != ReceiptStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        List<PurReceiptItem> items = doc.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("收货明细不能为空,无法提交");
        }
        // 回写明细小计:amount = receivedQty × unitPrice(receivedQty 为空时按 orderedQty 预估)
        for (PurReceiptItem it : items) {
            if (it.getAmount() == null) {
                BigDecimal q = it.getReceivedQty() != null ? it.getReceivedQty() : it.getOrderedQty();
                if (q == null) q = BigDecimal.ZERO;
                BigDecimal p = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                it.setAmount(q.multiply(p));
            }
        }
        doc.setStatus(ReceiptStatus.SUBMITTED.code);
    }

    /** 收货:已提交 → 已收货,校验实收数量已填 + 回写主单 receivedQty/receivedAmount/receivedAt/receiver。 */
    private void applyReceive(PurPurchaseReceipt doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != ReceiptStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可收货,当前状态码: " + st);
        }
        List<PurReceiptItem> items = doc.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("收货明细不能为空,无法收货");
        }
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurReceiptItem it : items) {
            BigDecimal q = it.getReceivedQty();
            if (q == null) {
                throw new IllegalStateException("明细[" + it.getItemCode() + "]实收数量未填,无法收货");
            }
            BigDecimal p = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
            BigDecimal amt = q.multiply(p);
            it.setAmount(amt);
            totalQty = totalQty.add(q);
            totalAmount = totalAmount.add(amt);
        }
        doc.setReceivedQty(totalQty);
        doc.setReceivedAmount(totalAmount);
        if (doc.getReceiptDate() == null) {
            doc.setReceiptDate(LocalDate.now());
        }
        doc.setReceivedAt(LocalDateTime.now());
        if (doc.getReceiver() == null || doc.getReceiver().isBlank()) {
            doc.setReceiver("system");
        }
        doc.setStatus(ReceiptStatus.RECEIVED.code);
    }

    /** 取消:草稿/已提交 → 已取消;已收货终态不可取消(已入库形成库存)。 */
    private void applyCancel(PurPurchaseReceipt doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == ReceiptStatus.RECEIVED.code) {
            throw new IllegalStateException("已收货不可取消(已形成库存),当前状态码: " + st);
        }
        if (st == ReceiptStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(ReceiptStatus.CANCELLED.code);
    }
}
