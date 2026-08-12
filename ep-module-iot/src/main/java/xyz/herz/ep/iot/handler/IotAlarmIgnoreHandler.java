package xyz.herz.ep.iot.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.iot.entity.IotAlarm;
import xyz.herz.ep.iot.entity.IotAlarmLog;
import xyz.herz.ep.iot.enums.IotDictEnums.AlarmStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警「忽略」行按钮:待处理(0)/处理中(10)→已忽略(30),并写告警日志。
 * <p>强校验:仅 PENDING / PROCESSING 状态可忽略,其他状态抛 IllegalStateException。
 */
@Component
public class IotAlarmIgnoreHandler implements OperationHandler<Object, Object> {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0;
        for (Object row : data) {
            IotAlarm a = (IotAlarm) row;
            Integer cur = a.getStatus();
            if (cur == null || (cur != AlarmStatus.PENDING.code && cur != AlarmStatus.PROCESSING.code)) {
                throw new IllegalStateException(
                    "告警[id=" + a.getId() + "] 当前状态=" + label(cur) + ",仅待处理/处理中状态可忽略");
            }
            a.setStatus(AlarmStatus.IGNORED.code);
            em.merge(a);
            writeLog(a, cur, AlarmStatus.IGNORED.code, "忽略");
            ok++;
        }
        return "忽略成功 " + ok + " 条";
    }

    private void writeLog(IotAlarm a, Integer from, Integer to, String action) {
        IotAlarmLog log = new IotAlarmLog();
        log.setAlarm(a);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setRemark(action);
        log.setCreateTime(LocalDateTime.now());
        em.persist(log);
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (AlarmStatus s : AlarmStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
