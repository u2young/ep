package xyz.herz.ep.fin.handler.budget;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.budget.FinBudget;
import xyz.herz.ep.fin.enums.FinDictEnums.BudgetStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 预算「批准」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → APPROVED(1)。
 */
@Component
public class FinBudgetApproveHandler implements OperationHandler<Object, Object> {

    public static final String CODE_APPROVE = "fin.budget.approve";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinBudget doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持预算; ");
                continue;
            }
            try {
                if (doc.getStatus() == null || doc.getStatus() != BudgetStatus.DRAFT.code) {
                    throw new IllegalStateException("只有草稿状态可以批准,当前状态=" + doc.getStatus());
                }
                doc.setStatus(BudgetStatus.APPROVED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("预算#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_APPROVE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
