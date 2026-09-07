package xyz.herz.ep.fin.handler.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.fin.enums.FinDictEnums.InvoiceStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售发票「提交」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → SUBMITTED(1)。
 * <p>触发 GL 凭证生成(经 FinPostingFacade 可选注入):
 * <ul>
 *   <li>借:应收账款(accountCode="AR")</li>
 *   <li>贷:主营业务收入(accountCode="REV")</li>
 * </ul>
 */
@Component
public class FinSalesInvoiceSubmitHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "fin.sales_invoice.submit";

    @PersistenceContext private EntityManager em;

    /** 可选注入:若 FinPostingService bean 存在则触发 GL 凭证生成;测试环境无 bean 时跳过 */
    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinSalesInvoice doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持销售发票; ");
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

                // 触发 GL 凭证:借应收/贷收入
                if (postingFacade != null) {
                    Long jeId = postingFacade.post(new FinPostingFacade.PostingRequest(
                        JournalSourceType.SALES_INVOICE.code,
                        doc.getId(),
                        doc.getNo(),
                        doc.getPostingDate() == null ? LocalDate.now() : doc.getPostingDate(),
                        List.of(
                            new FinPostingFacade.PostingLine("AR", total, null,
                                "Customer", doc.getCustomerId(), doc.getCustomerName(), null, "应收账款"),
                            new FinPostingFacade.PostingLine("REV", null, total,
                                null, null, null, null, "主营业务收入")
                        ),
                        "销售发票 " + doc.getNo() + " 自动入账"
                    ));
                    // 可选:回写 invoice.generatedJournalId(本实体当前未声明该字段,留扩展)
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("销售发票#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_SUBMIT);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
