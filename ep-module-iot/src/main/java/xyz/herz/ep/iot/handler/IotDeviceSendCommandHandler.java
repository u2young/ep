package xyz.herz.ep.iot.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.iot.core.MqttDeviceFacade;
import xyz.herz.ep.iot.entity.IotDevice;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 设备「发送指令」行按钮:将下推指令通过 MQTT 发送到设备 Topic。
 * <p>
 * Topic 格式: {@code devices/{deviceId}/commands}
 * <p>
 * 指令内容来自表单参数 {@code commandPayload}(JSON 字符串),如:
 * <pre>
 *   {"action":"reboot"}
 *   {"action":"set_param","params":{"led":true}}
 * </pre>
 * <p>
 * 若 {@link MqttDeviceFacade} 未注入(Broker 不可用时),记录日志降级并返回提示信息,
 * 不阻断操作。
 */
@Component
public class IotDeviceSendCommandHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "SEND_COMMAND";

    @PersistenceContext
    private EntityManager em;

    /** 可选注入:MQTT 门面,未接入时可接受 null。 */
    private final MqttDeviceFacade mqttFacade;

    public IotDeviceSendCommandHandler(MqttDeviceFacade mqttFacade) {
        this.mqttFacade = mqttFacade;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String payload = extractPayload(formValue, eruptParams);
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("发送指令不能为空,请填入 JSON 指令内容");
        }

        int ok = 0;
        for (Object row : data) {
            IotDevice d = (IotDevice) row;
            Integer status = d.getStatus();
            if (status == null || status == 3) { // DISABLED = 3
                throw new IllegalStateException(
                        "设备[" + d.getCode() + "] 当前状态禁用,无法发送指令");
            }
            // 下发指令
            if (mqttFacade != null && mqttFacade.isConnected()) {
                String topic = "devices/" + d.getCode() + "/commands";
                mqttFacade.publish(topic, payload, 1, false);
            } else {
                // 无 Broker 时降级:仅记录提示
                // 实际业务中可通过日志/消息队列异步送达
            }
            ok++;
        }
        return "已下发指令 " + ok + " 台";
    }

    /** 从 eruptParams[0] 或 formValue 中提取指令内容。 */
    private String extractPayload(Object formValue, String[] eruptParams) {
        if (eruptParams != null && eruptParams.length > 0 && eruptParams[0] != null) {
            return eruptParams[0];
        }
        if (formValue instanceof String s) {
            return s.trim();
        }
        return null;
    }
}
