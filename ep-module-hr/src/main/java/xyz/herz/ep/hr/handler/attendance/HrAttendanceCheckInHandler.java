package xyz.herz.ep.hr.handler.attendance;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.hr.entity.attendance.HrAttendance;
import xyz.herz.ep.hr.enums.HrDictEnums.AttendanceStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤签到/签退行按钮处理器(参考 ERPNext Attendance 签到模型)。
 * <p>签到:缺勤/请假 → 出勤,写 inTime;签退:出勤 → 写 outTime + 派生 workHours(小时,2 位)。
 */
@Component
public class HrAttendanceCheckInHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CHECK_IN = "hr.attendance.check_in";
    public static final String CODE_CHECK_OUT = "hr.attendance.check_out";

    private static final BigDecimal SIXTY = new BigDecimal("60");

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_CHECK_IN;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof HrAttendance doc)) {
                fail++; sb.append("仅支持考勤实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_CHECK_IN -> applyCheckIn(doc);
                    case CODE_CHECK_OUT -> applyCheckOut(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("考勤#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 签到:缺勤/请假 → 出勤,记录签到时间。 */
    private void applyCheckIn(HrAttendance doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != AttendanceStatus.ABSENT.code && st != AttendanceStatus.ON_LEAVE.code) {
            throw new IllegalStateException("仅缺勤/请假状态可签到,当前状态码: " + st);
        }
        doc.setStatus(AttendanceStatus.PRESENT.code);
        doc.setInTime(LocalDateTime.now());
    }

    /** 签退:出勤 → 写签退时间 + 派生工时(小时)。 */
    private void applyCheckOut(HrAttendance doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != AttendanceStatus.PRESENT.code) {
            throw new IllegalStateException("仅出勤状态可签退,当前状态码: " + st);
        }
        if (doc.getInTime() == null) {
            throw new IllegalStateException("签到时间为空,无法签退计算工时");
        }
        LocalDateTime out = LocalDateTime.now();
        doc.setOutTime(out);
        long minutes = Duration.between(doc.getInTime(), out).toMinutes();
        if (minutes < 0) {
            throw new IllegalStateException("签退时间早于签到时间,数据异常");
        }
        doc.setWorkHours(new BigDecimal(minutes).divide(SIXTY, 2, java.math.RoundingMode.HALF_UP));
    }
}
