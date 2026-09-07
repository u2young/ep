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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购发票「提交」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → SUBMITTED(1)。
 * <p>触发 GL 凭证生成:借采购费用("EXP")/贷应付账款("AP")。
 */
@Component
public class FinPurchaseInvoiceSubmitHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "fin.purchase_invoice.submit";

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
                if (doc.getStatus() == null || doc.getStatus() != InvoiceStatus.DRAFT.code) {
                    throw new IllegalStateException("只有草稿状态可以提交,当前状态=" + doc.getStatus());
                }
                BigDecimal total = doc.getTotal() == null ? BigDecimal.ZERO : doc.getTotal();
                if (total.signum() <= 0) {
                    throw new IllegalStateException("发票金额必须 > 0");
                }
                doc.setStatus(InvoiceStatus.SUBMITTED.code);
                em.merge(doc);

                if (postingFacade != null) {
                    postingFacade.post(new FinPostingFacade.PostingRequest(
                        JournalSourceType.PURCHASE_INVOICE.code,
                        doc.getId(),
                        doc.getNo(),
                        doc.getPostingDate() == null ? LocalDate.now() : doc.getPostingDate(),
                        List.of(
                            new FinPostingFacade.PostingLine("EXP", total, null,
                                "Supplier", doc.getSupplierId(), doc.getSupplierName(), null, "采购费用"),
                            new FinPostingFacade.PostingLine("AP", null, total,
                                "Supplier", doc.getSupplierId(), doc.getSupplierName(), null, "应付账款")
                        ),
                        "采购发票 " + doc.getNo() + " 自动入账"
                    ));
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("采购发票#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_SUBMIT);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
