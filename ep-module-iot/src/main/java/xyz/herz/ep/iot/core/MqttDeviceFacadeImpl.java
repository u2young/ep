package xyz.herz.ep.iot.core;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 设备通信门面实现,封装 Eclipse Paho MQTTv5 Client。
 * <p>
 * 采用单客户端模式(默认 broker: {@code tcp://localhost:1883}),支持发布、订阅与自动重连。
 * 每个订阅通过内部 {@link MqttSubscriptionImpl} 维护,应用关闭时统一断开连接。
 */
@Service
@ConditionalOnProperty(name = "ep.iot.mqtt.mock", havingValue = "false")
public class MqttDeviceFacadeImpl implements MqttDeviceFacade {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceFacadeImpl.class);

    private static final String DEFAULT_BROKER = "tcp://localhost:1883";
    private static final String CLIENT_ID_PREFIX = "ep-iot-";

    private final MqttAsyncClient client;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, MqttSubscriptionImpl> subscriptions = new ConcurrentHashMap<>();

    public MqttDeviceFacadeImpl() {
        this(DEFAULT_BROKER);
    }

    public MqttDeviceFacadeImpl(String brokerUri) {
        String clientId = CLIENT_ID_PREFIX + System.nanoTime();
        try {
            client = new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            options.setKeepAliveInterval(60);
            client.connect(options);
            connected.set(true);
            log.info("MQTT v5 client connected broker={} clientId={}", brokerUri, clientId);
        } catch (MqttException e) {
            log.warn("MQTT 连接失败 broker={},功能降级: {}", brokerUri, e.getMessage());
            throw new RuntimeException("MQTT 连接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String topic, String payload, int qos, boolean retain) {
        if (topic == null || topic.isBlank() || payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("topic 和 payload 不能为空");
        }
        if (!connected.get()) {
            throw new IllegalStateException("MQTT 客户端未连接,无法发布");
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retain);
            client.publish(topic, message);
        } catch (MqttException e) {
            throw new RuntimeException("MQTT 发布失败 topic=" + topic, e);
        }
    }

    @Override
    public MqttSubscription subscribe(String topic, int qos, MqttMessageHandler messageHandler) {
        if (!connected.get()) {
            throw new IllegalStateException("MQTT 客户端未连接,无法订阅");
        }
        try {
            // Paho v5: create MqttSubscription object with topic and qos
            org.eclipse.paho.mqttv5.common.MqttSubscription pahoSub =
                    new org.eclipse.paho.mqttv5.common.MqttSubscription(topic, qos);
            IMqttMessageListener listener = (t, msg) -> {
                if (messageHandler != null) {
                    messageHandler.onMessage(t.toString(), new String(msg.getPayload()));
                }
            };
            // subscribe with message listener
            client.subscribe(pahoSub, null, null, listener, new MqttProperties());
            MqttSubscriptionImpl sub = new MqttSubscriptionImpl(client, topic);
            subscriptions.put(topic, sub);
            log.info("MQTT 订阅成功 topic={} qos={}", topic, qos);
            return sub;
        } catch (MqttException e) {
            throw new RuntimeException("MQTT 订阅失败 topic=" + topic, e);
        }
    }

    @Override
    public void disconnect() {
        subscriptions.values().forEach(MqttSubscription::unsubscribe);
        subscriptions.clear();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
            connected.set(false);
            log.info("MQTT 客户端已断开");
        } catch (MqttException e) {
            log.warn("MQTT 断开连接异常: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void onApplicationShutdown() {
        disconnect();
    }

    @Override
    public boolean isConnected() {
        return connected.get() && client != null && client.isConnected();
    }

    // ==================== 内部实现类 ====================

    private static class MqttSubscriptionImpl implements MqttSubscription {
        private final MqttAsyncClient client;
        private final String topic;

        MqttSubscriptionImpl(MqttAsyncClient client, String topic) {
            this.client = client;
            this.topic = topic;
        }

        @Override
        public void unsubscribe() {
            try {
                client.unsubscribe(topic);
                log.info("MQTT 取消订阅 topic={}", topic);
            } catch (MqttException e) {
                log.warn("MQTT 取消订阅失败 topic={}: {}", topic, e.getMessage());
            }
        }
    }
}
