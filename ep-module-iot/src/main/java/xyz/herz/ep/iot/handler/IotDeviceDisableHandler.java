package xyz.herz.ep.iot.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.iot.entity.IotDevice;
import xyz.herz.ep.iot.enums.IotDictEnums.DeviceStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 设备「禁用」行按钮:在线(1)/离线(2)→禁用(3)。
 * <p>强校验:仅 ONLINE / OFFLINE 状态可禁用,其他状态抛 IllegalStateException。
 */
@Component
public class IotDeviceDisableHandler implements OperationHandler<Object, Object> {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0;
        for (Object row : data) {
            IotDevice d = (IotDevice) row;
            Integer cur = d.getStatus();
            if (cur == null || (cur != DeviceStatus.ONLINE.code && cur != DeviceStatus.OFFLINE.code)) {
                throw new IllegalStateException(
                    "设备[" + d.getCode() + "] 当前状态=" + label(cur) + ",仅在线/离线状态可禁用");
            }
            d.setStatus(DeviceStatus.DISABLED.code);
            em.merge(d);
            ok++;
        }
        return "禁用成功 " + ok + " 台";
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (DeviceStatus s : DeviceStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
