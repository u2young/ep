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
 * 设备「启用」行按钮:禁用(3)→离线(2)。
 * <p>强校验:仅 DISABLED 状态可启用,其他状态抛 IllegalStateException。
 */
@Component
public class IotDeviceEnableHandler implements OperationHandler<Object, Object> {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0;
        for (Object row : data) {
            IotDevice d = (IotDevice) row;
            Integer cur = d.getStatus();
            if (cur == null || cur != DeviceStatus.DISABLED.code) {
                throw new IllegalStateException(
                    "设备[" + d.getCode() + "] 当前状态=" + label(cur) + ",仅禁用状态可启用");
            }
            d.setStatus(DeviceStatus.OFFLINE.code);
            em.merge(d);
            ok++;
        }
        return "启用成功 " + ok + " 台";
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (DeviceStatus s : DeviceStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
