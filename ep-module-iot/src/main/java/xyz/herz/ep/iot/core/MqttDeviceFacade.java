package xyz.herz.ep.iot.core;

/**
 * MQTT 设备通信门面。
 * <p>
 * 封装 Eclipse Paho MQTTv5 Client,对外隐藏第三方依赖,便于测试替换。
 * <p>
 * 核心功能:
 * <ul>
 *   <li>发布设备遥测数据(Telemetry)到 Topic</li>
 *   <li>订阅设备影子(Shadow)变更 Topic,推送服务端状态给设备</li>
 *   <li>接收设备上下行命令(Command)响应</li>
 *   <li>EMQX 设备在线/离线状态感知</li>
 * </ul>
 *
 * @see <a href="https://projects.eclipse.org/projects/iot.paho">Eclipse Paho</a>
 */
public interface MqttDeviceFacade {

    /**
     * 向指定 Topic 发布消息( QoS 1,保留标志可选)。
     *
     * @param topic    目标 Topic(如 {@code devices/{deviceId}/telemetry})
     * @param payload  消息体(JSON 字符串)
     * @param qos      服务质量: 0=至多一次 / 1=至少一次 / 2=恰好一次
     * @param retain   是否保留最后一条消息(用于设备影子)
     * @throws IllegalArgumentException 当 topic 或 payload 为空时抛出
     */
    void publish(String topic, String payload, int qos, boolean retain);

    /**
     * 订阅主题并注册消息处理器。
     * <p>
     * 适用于订阅设备影子 Topic、云端指令 Topic 等。
     *
     * @param topic       订阅主题(支持通配符 +/+)
     * @param qos         订阅 QoS
     * @param messageHan  dler 消息回调 ({@code (topic, payload) -> ...})
     * @return 订阅句柄,可用于 unsubscribe
     */
    MqttSubscription subscribe(String topic, int qos,
                               MqttMessageHandler messageHandler);

    /**
     * 断开客户端连接并清理资源。
     * <p>
     * 必须在应用关闭时调用,否则连接泄漏。
     */
    void disconnect();

    /**
     * 检查客户端是否已连接。
     */
    boolean isConnected();

    /**
     * MQTT 消息处理器函数式接口。
     */
    @FunctionalInterface
    interface MqttMessageHandler {
        void onMessage(String topic, String payload);
    }

    /**
     * MQTT 订阅句柄,用于取消订阅。
     */
    interface MqttSubscription {
        /** 取消该订阅 */
        void unsubscribe();
    }
}
