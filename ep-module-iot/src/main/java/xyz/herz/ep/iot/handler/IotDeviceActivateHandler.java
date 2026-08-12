package xyz.herz.ep.iot.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.iot.entity.IotDevice;
import xyz.herz.ep.iot.enums.IotDictEnums.DeviceStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * 设备「激活」行按钮:未激活(0)→离线(2),并生成 deviceSecret。
 * <p>强校验:仅 INACTIVE 状态可激活,其他状态抛 IllegalStateException。
 */
@Component
public class IotDeviceActivateHandler implements OperationHandler<Object, Object> {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0;
        for (Object row : data) {
            IotDevice d = (IotDevice) row;
            Integer cur = d.getStatus();
            if (cur == null || cur != DeviceStatus.INACTIVE.code) {
                throw new IllegalStateException(
                    "设备[" + d.getCode() + "] 当前状态=" + label(cur) + ",仅未激活状态可激活");
            }
            d.setStatus(DeviceStatus.OFFLINE.code);
            if (d.getDeviceSecret() == null || d.getDeviceSecret().isEmpty()) {
                d.setDeviceSecret(UUID.randomUUID().toString().replace("-", ""));
            }
            em.merge(d);
            ok++;
        }
        return "激活成功 " + ok + " 台";
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (DeviceStatus s : DeviceStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
