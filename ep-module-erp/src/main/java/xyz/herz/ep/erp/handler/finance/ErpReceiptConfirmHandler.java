package xyz.herz.ep.erp.handler.finance;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.erp.entity.finance.ErpFinanceReceipt;
import xyz.herz.ep.erp.enums.ErpDictEnums.FinanceDocStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 收款单「确认」行按钮处理器。
 * <p>状态迁移:0(草稿) → 1(已确认)。
 */
@Component
public class ErpReceiptConfirmHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CONFIRM = "erp.finance.receipt.confirm";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ErpFinanceReceipt doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持收款单; ");
                continue;
            }
            try {
                if (doc.getStatus() == null || doc.getStatus() != FinanceDocStatus.DRAFT.code) {
                    throw new IllegalStateException("只有草稿状态可以确认,当前状态=" + label(doc.getStatus()));
                }
                doc.setStatus(FinanceDocStatus.CONFIRMED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(ErpFinanceReceipt.class.getSimpleName()).append("#").append(doc.getNo())
                  .append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CONFIRM);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (FinanceDocStatus s : FinanceDocStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
