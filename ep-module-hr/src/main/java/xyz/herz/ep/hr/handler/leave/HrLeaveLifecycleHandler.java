package xyz.herz.ep.hr.handler.leave;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.hr.entity.leave.HrLeaveApplication;
import xyz.herz.ep.hr.enums.HrDictEnums.LeaveStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 请假单生命周期行按钮处理器(参考 ERPNext Leave Application 工作流)。
 * <p>申请(DRAFT→APPLIED,派生 totalDays) / 批准(APPLIED→APPROVED)
 * / 拒绝(APPLIED→REJECTED) / 取消(非 CANCELLED→CANCELLED)。
 */
@Component
public class HrLeaveLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_APPLY = "hr.leave.apply";
    public static final String CODE_APPROVE = "hr.leave.approve";
    public static final String CODE_REJECT = "hr.leave.reject";
    public static final String CODE_CANCEL = "hr.leave.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_APPLY;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof HrLeaveApplication doc)) {
                fail++; sb.append("仅支持请假单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_APPLY -> applyApply(doc);
                    case CODE_APPROVE -> applyApprove(doc);
                    case CODE_REJECT -> applyReject(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("请假单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 申请:草稿 → 已申请,派生 totalDays。 */
    private void applyApply(HrLeaveApplication doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != LeaveStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可申请,当前状态码: " + st);
        }
        if (doc.getFromDate() == null || doc.getToDate() == null) {
            throw new IllegalStateException("开始/结束日期不能为空");
        }
        if (doc.getToDate().isBefore(doc.getFromDate())) {
            throw new IllegalStateException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(doc.getFromDate(), doc.getToDate()) + 1;
        doc.setTotalDays(BigDecimal.valueOf(days));
        doc.setStatus(LeaveStatus.APPLIED.code);
    }

    /** 批准:已申请 → 已批准。 */
    private void applyApprove(HrLeaveApplication doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != LeaveStatus.APPLIED.code) {
            throw new IllegalStateException("仅已申请状态可批准,当前状态码: " + st);
        }
        doc.setStatus(LeaveStatus.APPROVED.code);
    }

    /** 拒绝:已申请 → 已拒绝。 */
    private void applyReject(HrLeaveApplication doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != LeaveStatus.APPLIED.code) {
            throw new IllegalStateException("仅已申请状态可拒绝,当前状态码: " + st);
        }
        doc.setStatus(LeaveStatus.REJECTED.code);
    }

    /** 取消:非已取消 → 已取消(草稿/已申请/已批准均可取消)。 */
    private void applyCancel(HrLeaveApplication doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == LeaveStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        if (st == LeaveStatus.REJECTED.code) {
            throw new IllegalStateException("已拒绝单据不可取消,请重新申请");
        }
        doc.setStatus(LeaveStatus.CANCELLED.code);
    }
}
