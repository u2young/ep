package xyz.herz.ep.sup.handler.issue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.sup.core.SupSlaEvaluator;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sup.enums.SupDictEnums.IssueStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户工单生命周期行按钮处理器(回复/解决/关闭/重开/取消,五合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>REPLY:OPEN(0)/REOPENED(4) → REPLIED(1),首次回复记 firstResponseAt;
 *       若 responseBy 未设则按 createTime 补算 SLA 截止。</li>
 *   <li>RESOLVE:REPLIED(1)/OPEN(0)/REOPENED(4) → RESOLVED(2),记 resolvedAt +
 *       slaFulfilled = SupSlaEvaluator.computeFulfillment。</li>
 *   <li>CLOSE:RESOLVED(2) → CLOSED(3),记 closedAt。</li>
 *   <li>REOPEN:CLOSED(3)/RESOLVED(2)/CANCELLED(5) → REOPENED(4),以 now 重算 SLA 截止,
 *       清 resolvedAt/closedAt/slaFulfilled。</li>
 *   <li>CANCEL:非 CLOSED/CANCELLED → CANCELLED(5);已关闭/已取消不可取消。</li>
 * </ul>
 */
@Component
public class SupIssueLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_REPLY = "sup.issue.reply";
    public static final String CODE_RESOLVE = "sup.issue.resolve";
    public static final String CODE_CLOSE = "sup.issue.close";
    public static final String CODE_REOPEN = "sup.issue.reopen";
    public static final String CODE_CANCEL = "sup.issue.cancel";

    @PersistenceContext private EntityManager em;
    private final SupSlaEvaluator slaEvaluator;

    public SupIssueLifecycleHandler(SupSlaEvaluator slaEvaluator) {
        this.slaEvaluator = slaEvaluator;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_REPLY;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof SupIssue doc)) {
                fail++; sb.append("仅支持工单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_REPLY -> applyReply(doc);
                    case CODE_RESOLVE -> applyResolve(doc);
                    case CODE_CLOSE -> applyClose(doc);
                    case CODE_REOPEN -> applyReopen(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyReply(SupIssue doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != IssueStatus.OPEN.code && st != IssueStatus.REOPENED.code)) {
            throw new IllegalStateException("只有打开/重新打开状态可回复,当前状态=" + st);
        }
        // 若 SLA 截止未设(新建工单未走 SLA 引擎),按 createTime 补算
        if (doc.getResponseBy() == null) {
            LocalDateTime baseline = doc.getCreateTime() != null ? doc.getCreateTime() : LocalDateTime.now();
            slaEvaluator.applyDeadlines(doc, baseline);
        }
        if (doc.getFirstResponseAt() == null) {
            doc.setFirstResponseAt(LocalDateTime.now());
        }
        doc.setStatus(IssueStatus.REPLIED.code);
    }

    private void applyResolve(SupIssue doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != IssueStatus.REPLIED.code && st != IssueStatus.OPEN.code
                && st != IssueStatus.REOPENED.code)) {
            throw new IllegalStateException("只有已回复/打开/重新打开状态可解决,当前状态=" + st);
        }
        if (doc.getResponseBy() == null) {
            LocalDateTime baseline = doc.getCreateTime() != null ? doc.getCreateTime() : LocalDateTime.now();
            slaEvaluator.applyDeadlines(doc, baseline);
        }
        doc.setResolvedAt(LocalDateTime.now());
        doc.setStatus(IssueStatus.RESOLVED.code);
        doc.setSlaFulfilled(slaEvaluator.computeFulfillment(doc));
    }

    private void applyClose(SupIssue doc) {
        Integer st = doc.getStatus();
        if (st == null || st != IssueStatus.RESOLVED.code) {
            throw new IllegalStateException("只有已解决状态可关闭,当前状态=" + st);
        }
        doc.setClosedAt(LocalDateTime.now());
        doc.setStatus(IssueStatus.CLOSED.code);
    }

    private void applyReopen(SupIssue doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != IssueStatus.CLOSED.code && st != IssueStatus.RESOLVED.code
                && st != IssueStatus.CANCELLED.code)) {
            throw new IllegalStateException("只有已关闭/已解决/已取消状态可重开,当前状态=" + st);
        }
        doc.setStatus(IssueStatus.REOPENED.code);
        // 重算 SLA 截止(以重开时间为基线),清解决/关闭时间与达成标记
        slaEvaluator.applyDeadlines(doc, LocalDateTime.now());
        doc.setResolvedAt(null);
        doc.setClosedAt(null);
        doc.setSlaFulfilled(null);
    }

    private void applyCancel(SupIssue doc) {
        Integer st = doc.getStatus();
        if (st == null || st == IssueStatus.CLOSED.code || st == IssueStatus.CANCELLED.code) {
            throw new IllegalStateException("已关闭/已取消工单不可取消,当前状态=" + st);
        }
        doc.setStatus(IssueStatus.CANCELLED.code);
    }
}
