package xyz.herz.ep.mfg.handler.jobcard;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.entity.jobcard.MfgJobCard;
import xyz.herz.ep.mfg.enums.MfgDictEnums.JobCardStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 派工单生命周期行按钮处理器(开工/完工/取消,三合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>START:PENDING(0) → IN_PRODUCTION(1),记 startedAt</li>
 *   <li>COMPLETE:IN_PRODUCTION(1) → COMPLETED(2),记 completedAt + totalTimeInMins</li>
 *   <li>CANCEL:PENDING(0)/IN_PRODUCTION(1) → CANCELLED(3)</li>
 * </ul>
 * 通过 {@code operationParam = { MfgJobCardLifecycleHandler.CODE_START }} 分派。
 */
@Component
public class MfgJobCardLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_START = "mfg.jobcard.start";
    public static final String CODE_COMPLETE = "mfg.jobcard.complete";
    public static final String CODE_CANCEL = "mfg.jobcard.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_START;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof MfgJobCard doc)) {
                fail++; sb.append("仅支持派工单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_START -> applyStart(doc);
                    case CODE_COMPLETE -> applyComplete(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("派工#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyStart(MfgJobCard doc) {
        Integer st = doc.getStatus();
        if (st == null || st != JobCardStatus.PENDING.code) {
            throw new IllegalStateException("只有待开工状态可以开工,当前状态=" + st);
        }
        doc.setStatus(JobCardStatus.IN_PRODUCTION.code);
        if (doc.getStartedAt() == null) {
            doc.setStartedAt(LocalDateTime.now());
        }
    }

    private void applyComplete(MfgJobCard doc) {
        Integer st = doc.getStatus();
        if (st == null || st != JobCardStatus.IN_PRODUCTION.code) {
            throw new IllegalStateException("只有在制状态可以完工,当前状态=" + st);
        }
        doc.setStatus(JobCardStatus.COMPLETED.code);
        doc.setCompletedAt(LocalDateTime.now());
        // 工时回写:若未手动填,按 startedAt → completedAt 差值计算分钟
        if ((doc.getTotalTimeInMins() == null || doc.getTotalTimeInMins().signum() <= 0)
                && doc.getStartedAt() != null) {
            long mins = java.time.Duration.between(doc.getStartedAt(), doc.getCompletedAt()).toMinutes();
            doc.setTotalTimeInMins(BigDecimal.valueOf(mins));
        }
    }

    private void applyCancel(MfgJobCard doc) {
        Integer st = doc.getStatus();
        if (st == null || st == JobCardStatus.COMPLETED.code) {
            throw new IllegalStateException("已完工派工单不可取消,当前状态=" + st);
        }
        if (st == JobCardStatus.CANCELLED.code) {
            throw new IllegalStateException("派工单已取消");
        }
        doc.setStatus(JobCardStatus.CANCELLED.code);
    }
}
