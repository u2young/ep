package xyz.herz.ep.iot.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQTT 门面 Mock 实现,无需真实 Broker 即可运行。
 * <p>
 * 通过属性 {@code ep.iot.mqtt.mock=true} 激活;默认不启用(生产使用 {@link MqttDeviceFacadeImpl})。
 * <p>
 * 记录所有发布消息到 {@link #publishedMessages},用于测试断言。
 */
@Service
@ConditionalOnProperty(name = "ep.iot.mqtt.mock", havingValue = "true", matchIfMissing = true)
public class MockMqttDeviceFacade implements MqttDeviceFacade {

    private volatile boolean connected = true;
    private final List<String> publishedMessages = new CopyOnWriteArrayList<>();
    private final List<SubscriptionRecord> subscriptions = new ArrayList<>();

    @Override
    public void publish(String topic, String payload, int qos, boolean retain) {
        if (!connected) {
            throw new IllegalStateException("Mock MQTT 客户端已断开");
        }
        String msg = topic + "|" + qos + "|" + retain + "|" + payload;
        publishedMessages.add(msg);
    }

    @Override
    public MqttSubscription subscribe(String topic, int qos, MqttMessageHandler messageHandler) {
        subscriptions.add(new SubscriptionRecord(topic, qos, messageHandler));
        return () -> subscriptions.removeIf(s -> s.topic.equals(topic));
    }

    @Override
    public void disconnect() {
        connected = false;
        subscriptions.clear();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    /** 返回所有已发布的消息字符串,供测试断言使用。 */
    public List<String> getPublishedMessages() {
        return new ArrayList<>(publishedMessages);
    }

    public record SubscriptionRecord(String topic, int qos, MqttMessageHandler handler) { }
}
