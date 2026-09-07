package xyz.herz.ep.fin.handler.journal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证「提交」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → SUBMITTED(1)。
 * <p>平账校验:提交时校验 totalDebit == totalCredit(由 FinPostingService 已在构造时回写,
 * 直接提交的草稿凭证需手动构造 items 后由前端调,本处只做平账 assert)。
 */
@Component
public class FinJournalSubmitHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "fin.journal.submit";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinJournalEntry doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持凭证; ");
                continue;
            }
            try {
                if (doc.getStatus() == null || doc.getStatus() != JournalStatus.DRAFT.code) {
                    throw new IllegalStateException("只有草稿状态可以提交,当前状态=" + doc.getStatus());
                }
                // 平账校验
                BigDecimal debit = doc.getTotalDebit() == null ? BigDecimal.ZERO : doc.getTotalDebit();
                BigDecimal credit = doc.getTotalCredit() == null ? BigDecimal.ZERO : doc.getTotalCredit();
                if (debit.compareTo(credit) != 0) {
                    throw new IllegalStateException("凭证借贷不平:debit=" + debit + ", credit=" + credit);
                }
                if (debit.signum() <= 0) {
                    throw new IllegalStateException("凭证金额必须 > 0");
                }
                doc.setStatus(JournalStatus.SUBMITTED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("凭证#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_SUBMIT);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
