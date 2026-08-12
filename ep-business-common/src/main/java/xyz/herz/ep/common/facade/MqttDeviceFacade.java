package xyz.herz.ep.common.facade;

/**
 * MQTT 设备通信 Facade。
 * IoT 模块通过此接口与 MQTT Broker 交互，
 * 测试环境使用 Stub 实现，生产环境替换为 Eclipse Paho / Spring Integration MQTT 实现。
 */
public interface MqttDeviceFacade {

    /** 发布下行指令到设备 */
    boolean publishCommand(String deviceCode, String topic, String payload);

    /** 订阅设备上行消息 */
    boolean subscribeDevice(String deviceCode, String topic);

    /** 取消订阅 */
    boolean unsubscribeDevice(String deviceCode, String topic);

    /** 获取设备在线状态 */
    boolean isDeviceOnline(String deviceCode);

    /** 更新设备影子（desired 状态） */
    boolean updateDeviceShadow(String deviceCode, String shadowJson);
}
