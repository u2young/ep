package xyz.herz.ep.mfg.handler.workorder;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.WorkOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 工单「取消」行按钮处理器。
 * <p>状态迁移:DRAFT(0)/NOT_STARTED(1)/IN_PRODUCTION(2)/STOPPED(4) → CANCELLED(5)。
 * 已完工(COMPLETED)状态不可取消(需走反向冲销)。
 */
@Component
public class MfgWorkOrderCancelHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CANCEL = "mfg.workorder.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof MfgWorkOrder doc)) {
                fail++; sb.append("仅支持工单; "); continue;
            }
            try {
                Integer st = doc.getStatus();
                if (st == null || st == WorkOrderStatus.COMPLETED.code) {
                    throw new IllegalStateException("已完工工单不可取消,当前状态=" + st);
                }
                if (st == WorkOrderStatus.CANCELLED.code) {
                    sb.append("工单 ").append(doc.getNo()).append(" 已取消; "); continue;
                }
                doc.setStatus(WorkOrderStatus.CANCELLED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工单#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CANCEL);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
