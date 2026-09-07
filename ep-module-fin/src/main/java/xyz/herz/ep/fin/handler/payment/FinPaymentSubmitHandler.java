package xyz.herz.ep.fin.handler.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.payment.FinPaymentEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentType;
import xyz.herz.ep.fin.facade.FinPostingFacade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收付款单「提交」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → SUBMITTED(1)。
 * <p>触发 GL 凭证生成,借贷方向看 paymentType:
 * <ul>
 *   <li>收款(RECEIVE):借银行("CASH")/贷应收("AR")</li>
 *   <li>付款(PAY):借应付("AP")/贷银行("CASH")</li>
 *   <li>内部转账(INTERNAL_TRANSFER):借目标账户("CASH")/贷源账户("CASH")</li>
 * </ul>
 */
@Component
public class FinPaymentSubmitHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "fin.payment.submit";

    @PersistenceContext private EntityManager em;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinPaymentEntry doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持收付款单; ");
                continue;
            }
            try {
                if (doc.getStatus() == null || doc.getStatus() != PaymentStatus.DRAFT.code) {
                    throw new IllegalStateException("只有草稿状态可以提交,当前状态=" + doc.getStatus());
                }
                BigDecimal amt = doc.getPaidAmount() == null ? BigDecimal.ZERO : doc.getPaidAmount();
                if (amt.signum() <= 0) {
                    throw new IllegalStateException("收付金额必须 > 0");
                }
                doc.setStatus(PaymentStatus.SUBMITTED.code);
                em.merge(doc);

                if (postingFacade != null) {
                    Integer pt = doc.getPaymentType() == null ? PaymentType.RECEIVE.code : doc.getPaymentType();
                    List<FinPostingFacade.PostingLine> lines;
                    if (pt == PaymentType.RECEIVE.code) {
                        lines = List.of(
                            new FinPostingFacade.PostingLine("CASH", amt, null,
                                doc.getPartyType(), doc.getPartyId(), doc.getPartyName(), null, "收款-银行"),
                            new FinPostingFacade.PostingLine("AR", null, amt,
                                doc.getPartyType(), doc.getPartyId(), doc.getPartyName(), null, "收款-冲应收")
                        );
                    } else if (pt == PaymentType.PAY.code) {
                        lines = List.of(
                            new FinPostingFacade.PostingLine("AP", amt, null,
                                doc.getPartyType(), doc.getPartyId(), doc.getPartyName(), null, "付款-冲应付"),
                            new FinPostingFacade.PostingLine("CASH", null, amt,
                                doc.getPartyType(), doc.getPartyId(), doc.getPartyName(), null, "付款-银行")
                        );
                    } else {
                        // 内部转账:借贷都用 CASH
                        lines = List.of(
                            new FinPostingFacade.PostingLine("CASH", amt, null, null, null, null, null, "内部转-借"),
                            new FinPostingFacade.PostingLine("CASH", null, amt, null, null, null, null, "内部转-贷")
                        );
                    }
                    Long jeId = postingFacade.post(new FinPostingFacade.PostingRequest(
                        JournalSourceType.PAYMENT_ENTRY.code,
                        doc.getId(),
                        doc.getNo(),
                        doc.getPostingDate() == null ? LocalDate.now() : doc.getPostingDate(),
                        lines,
                        "收付款单 " + doc.getNo() + " 自动入账"
                    ));
                    doc.setGeneratedJournalId(jeId);
                    em.merge(doc);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("收付款单#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_SUBMIT);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
