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
 * 工单「停工」行按钮处理器。
 * <p>状态迁移:IN_PRODUCTION(2) → STOPPED(4);停工后可通过开工恢复。
 */
@Component
public class MfgWorkOrderStopHandler implements OperationHandler<Object, Object> {

    public static final String CODE_STOP = "mfg.workorder.stop";

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
                if (st == null || st != WorkOrderStatus.IN_PRODUCTION.code) {
                    throw new IllegalStateException("只有在制状态可以停工,当前状态=" + st);
                }
                doc.setStatus(WorkOrderStatus.STOPPED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工单#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_STOP);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
