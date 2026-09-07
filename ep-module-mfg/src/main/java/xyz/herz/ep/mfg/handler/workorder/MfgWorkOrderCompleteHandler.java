package xyz.herz.ep.mfg.handler.workorder;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.WorkOrderStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单「完工」行按钮处理器。
 * <p>状态迁移:IN_PRODUCTION(2) → COMPLETED(3);记录 actualEnd。
 * <p>完工校验:producedQty >= qty(已完工数 >= 计划数量)。
 */
@Component
public class MfgWorkOrderCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "mfg.workorder.complete";

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
                    throw new IllegalStateException("只有在制状态可以完工,当前状态=" + st);
                }
                BigDecimal produced = doc.getProducedQty() == null ? BigDecimal.ZERO : doc.getProducedQty();
                BigDecimal planned = doc.getQty() == null ? BigDecimal.ZERO : doc.getQty();
                if (planned.signum() > 0 && produced.compareTo(planned) < 0) {
                    throw new IllegalStateException("已完工数 " + produced + " < 计划数 " + planned + ",不可完工");
                }
                doc.setStatus(WorkOrderStatus.COMPLETED.code);
                doc.setActualEnd(LocalDateTime.now());
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工单#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_COMPLETE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
