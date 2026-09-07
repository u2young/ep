package xyz.herz.ep.fin.handler.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.enums.FinDictEnums.InvoiceStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 采购发票「取消」行按钮处理器。
 * <p>状态迁移:DRAFT(0)/SUBMITTED(1) → CANCELLED(4)。
 * <p>对 SUBMITTED 状态的发票触发反向冲销 GL。
 */
@Component
public class FinPurchaseInvoiceCancelHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CANCEL = "fin.purchase_invoice.cancel";

    @PersistenceContext private EntityManager em;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinPurchaseInvoice doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持采购发票; ");
                continue;
            }
            try {
                if (doc.getStatus() == null) {
                    throw new IllegalStateException("发票状态为空");
                }
                if (doc.getStatus() == InvoiceStatus.CANCELLED.code) {
                    sb.append("发票 ").append(doc.getNo()).append(" 已取消; ");
                    continue;
                }
                Integer oldStatus = doc.getStatus();
                doc.setStatus(InvoiceStatus.CANCELLED.code);
                em.merge(doc);

                if (postingFacade != null && oldStatus != null && oldStatus == InvoiceStatus.SUBMITTED.code) {
                    postingFacade.cancel(JournalSourceType.PURCHASE_INVOICE.code, doc.getId());
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("采购发票#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CANCEL);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
