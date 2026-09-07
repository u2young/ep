package xyz.herz.ep.fin.handler.journal;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 凭证「取消」行按钮处理器。
 * <p>状态迁移:DRAFT(0)/SUBMITTED(1) → CANCELLED(2)。
 */
@Component
public class FinJournalCancelHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CANCEL = "fin.journal.cancel";

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
                if (doc.getStatus() == null) {
                    throw new IllegalStateException("凭证状态为空");
                }
                if (doc.getStatus() == JournalStatus.CANCELLED.code) {
                    sb.append("凭证 ").append(doc.getNo()).append(" 已取消; ");
                    continue;
                }
                doc.setStatus(JournalStatus.CANCELLED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("凭证#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CANCEL);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
