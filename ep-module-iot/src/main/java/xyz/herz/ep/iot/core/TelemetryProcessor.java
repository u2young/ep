package xyz.herz.ep.iot.core;

import org.springframework.stereotype.Service;
import xyz.herz.ep.iot.entity.IotAlarmRule;
import xyz.herz.ep.iot.entity.IotDevice;
import xyz.herz.ep.iot.entity.IotDeviceMessage;
import xyz.herz.ep.iot.enums.IotDictEnums.DeviceStatus;
import xyz.herz.ep.iot.enums.IotDictEnums.EnableStatus;
import xyz.herz.ep.iot.enums.IotDictEnums.ThingModelType;
import xyz.herz.ep.iot.jpa.IotAlarmRuleRepository;
import xyz.herz.ep.iot.jpa.IotDeviceMessageRepository;
import xyz.herz.ep.iot.jpa.IotDeviceRepository;
import xyz.herz.ep.iot.jpa.IotThingModelRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 遥测数据处理处理器。
 * <p>
 * 负责将设备通过 MQTT 上报的遥测数据(telemetry)持久化到
 * {@link IotDeviceMessage},并同步更新设备的 {@code lastOnlineTime} +
 * 在线状态{@link DeviceStatus#ONLINE}。
 * <p>
 * 典型调用方:
 * <ul>
 *   <li>{@link MqttDeviceFacade} 消息订阅回调(生产环境)</li>
 *   <li>测试或外部系统直接调用此方法写入历史数据</li>
 * </ul>
 *
 * <p>告警规则评估由 {@link xyz.herz.ep.iot.core.IotAlarmEvaluator} 负责,
 * 本处理器仅负责数据入库与设备在线状态同步。
 */
@Service
public class TelemetryProcessor {

    private final IotDeviceRepository deviceRepo;
    private final IotDeviceMessageRepository msgRepo;
    private final IotThingModelRepository thingModelRepo;

    public TelemetryProcessor(IotDeviceRepository deviceRepo,
                              IotDeviceMessageRepository msgRepo,
                              IotThingModelRepository thingModelRepo) {
        this.deviceRepo = deviceRepo;
        this.msgRepo = msgRepo;
        this.thingModelRepo = thingModelRepo;
    }

    /**
     * 处理单条遥测上报。
     *
     * @param deviceId  设备 ID
     * @param payload   原始 JSON 报文
     * @param messageId 消息唯一标识(可选,用于去重)
     * @return 写入的消息记录 ID
     */
    public Long process(Long deviceId, String payload, String messageId) {
        IotDevice device = deviceRepo.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在 id=" + deviceId));

        // 更新设备最后在线时间和在线状态
        device.setLastOnlineTime(LocalDateTime.now());
        if (device.getStatus() == DeviceStatus.OFFLINE.code) {
            device.setStatus(DeviceStatus.ONLINE.code);
        }
        deviceRepo.save(device);

        // 写入消息记录(上行 property 类型)
        IotDeviceMessage msg = new IotDeviceMessage();
        msg.setDevice(device);
        msg.setMessageId(messageId != null ? messageId : "auto-" + System.nanoTime());
        msg.setDirection(1); // 上行
        msg.setType(ThingModelType.PROPERTY.code + "");
        msg.setPayload(payload);
        msg.setTimestamp(LocalDateTime.now());
        msgRepo.save(msg);

        return msg.getId();
    }

    /**
     * 按物模型 identifier 批量处理遥测上报,用于一次上报包含多个属性值的场景。
     *
     * @param deviceId       设备 ID
     * @param telemetryJson  JSON 对象,如 {@code {"temperature":36.5,"humidity":70}}
     * @param messageId      消息唯一标识
     */
    public void processBatch(Long deviceId, String telemetryJson, String messageId) {
        process(deviceId, telemetryJson, messageId);
        // TODO: P2 解析 telemetryJson 按 identifier 分拆,逐条写消息 + 触发告警评估
    }

    /**
     * 根据设备 ID 查询最后一条遥测记录(用于仪表盘快速展示)。
     */
    public IotDeviceMessage lastTelemetry(Long deviceId) {
        return msgRepo.findAll().stream()
                .filter(m -> m.getDevice() != null && m.getDevice().getId().equals(deviceId))
                .max((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .orElse(null);
    }
}
