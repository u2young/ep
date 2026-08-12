package xyz.herz.ep.iot;

import xyz.herz.ep.iot.entity.*;
import xyz.herz.ep.iot.enums.IotDictEnums.*;
import xyz.herz.ep.iot.handler.*;
import xyz.herz.ep.iot.jpa.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IoT 模块冒烟单测:
 * <ol>
 *   <li>产品分类 + 产品 + 物模型 CRUD</li>
 *   <li>设备生命周期:未激活 → 激活(离线) → 禁用 → 启用(离线)</li>
 *   <li>告警全链路:创建告警 → 处理 → 解决;创建告警 → 忽略</li>
 *   <li>消息记录 CRUD</li>
 *   <li>状态机强校验:非法前置状态抛 IllegalStateException</li>
 * </ol>
 */
@SpringBootTest(classes = IotTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class IotSmokeTests {

    @Autowired IotProductCategoryRepository catRepo;
    @Autowired IotProductRepository productRepo;
    @Autowired IotThingModelRepository thingModelRepo;
    @Autowired IotDeviceRepository deviceRepo;
    @Autowired IotDeviceMessageRepository msgRepo;
    @Autowired IotAlarmRuleRepository ruleRepo;
    @Autowired IotAlarmRepository alarmRepo;
    @Autowired IotAlarmLogRepository alarmLogRepo;

    @Autowired IotProductToggleHandler productToggle;
    @Autowired IotDeviceActivateHandler deviceActivate;
    @Autowired IotDeviceEnableHandler deviceEnable;
    @Autowired IotDeviceDisableHandler deviceDisable;
    @Autowired IotAlarmProcessHandler alarmProcess;
    @Autowired IotAlarmResolveHandler alarmResolve;
    @Autowired IotAlarmIgnoreHandler alarmIgnore;

    // =================== 1. 产品分类 + 产品 + 物模型 CRUD ===================

    @Test
    void product_category_thing_model_crud() {
        IotProductCategory cat = new IotProductCategory();
        cat.setName("传感器");
        cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        assertNotNull(cat.getId());

        IotProduct p = new IotProduct();
        p.setName("温湿度计");
        p.setCode("P-" + System.nanoTime());
        p.setCategory(cat);
        p.setNodeType(NodeType.DIRECT.code);
        p.setNetType(NetType.WIFI.code);
        p.setStatus(EnableStatus.ENABLED.code);
        productRepo.save(p);
        assertNotNull(p.getId());
        assertEquals(NetType.WIFI.code, p.getNetType());

        IotThingModel tm = new IotThingModel();
        tm.setProduct(p);
        tm.setIdentifier("temperature");
        tm.setName("温度");
        tm.setType(ThingModelType.PROPERTY.code);
        tm.setDataType(DataType.FLOAT.code);
        tm.setAccessMode(AccessMode.READ_ONLY.code);
        tm.setSpecs("{\"min\":-40,\"max\":80,\"unit\":\"℃\"}");
        thingModelRepo.save(tm);
        assertNotNull(tm.getId());

        IotThingModel loaded = thingModelRepo.findById(tm.getId()).orElseThrow();
        assertEquals("temperature", loaded.getIdentifier());
        assertEquals(ThingModelType.PROPERTY.code, loaded.getType());
        assertEquals(DataType.FLOAT.code, loaded.getDataType());
        assertEquals(p.getId(), loaded.getProduct().getId());

        // 产品启停行按钮
        productToggle.exec(List.of(p), null, new String[]{ IotProductToggleHandler.DISABLE });
        assertEquals(EnableStatus.DISABLED.code,
            productRepo.findById(p.getId()).orElseThrow().getStatus());
        productToggle.exec(List.of(p), null, new String[]{ IotProductToggleHandler.ENABLE });
        assertEquals(EnableStatus.ENABLED.code,
            productRepo.findById(p.getId()).orElseThrow().getStatus());
    }

    // =================== 2. 设备生命周期: 未激活→激活(离线)→禁用→启用(离线) ===================

    @Test
    void device_lifecycle_activate_disable_enable() {
        IotProduct p = basicProduct();

        IotDevice d = new IotDevice();
        d.setProduct(p);
        d.setName("设备-A");
        d.setCode("DEV-" + System.nanoTime());
        d.setStatus(DeviceStatus.INACTIVE.code);
        deviceRepo.save(d);
        assertEquals(DeviceStatus.INACTIVE.code,
            deviceRepo.findById(d.getId()).orElseThrow().getStatus());
        assertNull(d.getDeviceSecret());

        // 激活:未激活(0)→离线(2),生成 deviceSecret
        deviceActivate.exec(List.of(d), null, new String[]{ "ACTIVATE" });
        IotDevice afterAct = deviceRepo.findById(d.getId()).orElseThrow();
        assertEquals(DeviceStatus.OFFLINE.code, afterAct.getStatus());
        assertNotNull(afterAct.getDeviceSecret(), "激活应生成 deviceSecret");

        // 禁用:在线/离线(1/2)→禁用(3)
        deviceDisable.exec(List.of(afterAct), null, new String[]{ "DISABLE" });
        assertEquals(DeviceStatus.DISABLED.code,
            deviceRepo.findById(d.getId()).orElseThrow().getStatus());

        // 启用:禁用(3)→离线(2)
        deviceEnable.exec(List.of(afterAct), null, new String[]{ "ENABLE" });
        assertEquals(DeviceStatus.OFFLINE.code,
            deviceRepo.findById(d.getId()).orElseThrow().getStatus());
    }

    // =================== 3. 告警全链路: 处理→解决; 忽略 ===================

    @Test
    void alarm_process_resolve_and_ignore() {
        IotProduct p = basicProduct();
        IotDevice d = new IotDevice();
        d.setProduct(p);
        d.setName("设备-B");
        d.setCode("DEV-" + System.nanoTime());
        d.setStatus(DeviceStatus.OFFLINE.code);
        deviceRepo.save(d);

        IotAlarmRule rule = new IotAlarmRule();
        rule.setProduct(p);
        rule.setDevice(d);
        rule.setIdentifier("temperature");
        rule.setName("高温告警");
        rule.setRuleType("threshold");
        rule.setCondition("{\"op\":\">\",\"field\":\"temperature\"}");
        rule.setThreshold(new BigDecimal("60"));
        rule.setStatus(EnableStatus.ENABLED.code);
        ruleRepo.save(rule);

        // 告警1:创建 → 处理 → 解决
        IotAlarm a1 = new IotAlarm();
        a1.setRule(rule);
        a1.setDevice(d);
        a1.setLevel(AlarmLevel.WARNING.code);
        a1.setStatus(AlarmStatus.PENDING.code);
        a1.setTriggerTime(LocalDateTime.now());
        alarmRepo.save(a1);

        alarmProcess.exec(List.of(a1), null, new String[]{ "PROCESS" });
        assertEquals(AlarmStatus.PROCESSING.code,
            alarmRepo.findById(a1.getId()).orElseThrow().getStatus());

        alarmResolve.exec(List.of(a1), null, new String[]{ "RESOLVE" });
        IotAlarm resolved = alarmRepo.findById(a1.getId()).orElseThrow();
        assertEquals(AlarmStatus.RESOLVED.code, resolved.getStatus());
        assertNotNull(resolved.getResolveTime(), "解决应设置 resolveTime");

        // 告警2:创建 → 忽略
        IotAlarm a2 = new IotAlarm();
        a2.setRule(rule);
        a2.setDevice(d);
        a2.setLevel(AlarmLevel.CRITICAL.code);
        a2.setStatus(AlarmStatus.PENDING.code);
        a2.setTriggerTime(LocalDateTime.now());
        alarmRepo.save(a2);

        alarmIgnore.exec(List.of(a2), null, new String[]{ "IGNORE" });
        assertEquals(AlarmStatus.IGNORED.code,
            alarmRepo.findById(a2.getId()).orElseThrow().getStatus());

        // 告警日志:告警1 应有 2 条(处理 + 解决),告警2 应有 1 条(忽略)
        long log1 = alarmLogRepo.findAll().stream()
            .filter(l -> a1.equals(l.getAlarm())).count();
        long log2 = alarmLogRepo.findAll().stream()
            .filter(l -> a2.equals(l.getAlarm())).count();
        assertEquals(2, log1, "告警1 应记录 2 条日志");
        assertEquals(1, log2, "告警2 应记录 1 条日志");
    }

    // =================== 4. 消息记录 CRUD ===================

    @Test
    void device_message_crud() {
        IotProduct p = basicProduct();
        IotDevice d = new IotDevice();
        d.setProduct(p);
        d.setName("设备-C");
        d.setCode("DEV-" + System.nanoTime());
        d.setStatus(DeviceStatus.ONLINE.code);
        deviceRepo.save(d);

        IotDeviceMessage up = new IotDeviceMessage();
        up.setDevice(d);
        up.setMessageId("msg-up-" + System.nanoTime());
        up.setDirection(1);
        up.setType("property");
        up.setPayload("{\"temperature\":36.5}");
        up.setTimestamp(LocalDateTime.now());
        msgRepo.save(up);
        assertNotNull(up.getId());

        IotDeviceMessage down = new IotDeviceMessage();
        down.setDevice(d);
        down.setMessageId("msg-down-" + System.nanoTime());
        down.setDirection(2);
        down.setType("service");
        down.setPayload("{\"action\":\"reboot\"}");
        down.setTimestamp(LocalDateTime.now());
        msgRepo.save(down);

        List<IotDeviceMessage> all = msgRepo.findAll();
        assertTrue(all.size() >= 2);

        IotDeviceMessage loaded = msgRepo.findById(up.getId()).orElseThrow();
        assertEquals(1, loaded.getDirection());
        assertEquals("property", loaded.getType());
        assertEquals(d.getCode(), loaded.getDevice().getCode());
    }

    // =================== 5. 状态机强校验(非法前置状态抛异常) ===================
    // 注:assertThrows 作为本方法最后操作,避免 rollback-only 影响后续断言。

    @Test
    void invalid_state_transitions_throw() {
        IotProduct p = basicProduct();

        // 激活一个「在线」设备 → 应抛异常(仅未激活可激活)
        IotDevice online = new IotDevice();
        online.setProduct(p);
        online.setName("dev-online");
        online.setCode("DEV-" + System.nanoTime());
        online.setStatus(DeviceStatus.ONLINE.code);
        deviceRepo.save(online);
        assertThrows(IllegalStateException.class,
            () -> deviceActivate.exec(List.of(online), null, new String[]{ "ACTIVATE" }));
    }

    // =================== helpers ===================

    private IotProduct basicProduct() {
        IotProductCategory cat = new IotProductCategory();
        cat.setName("cat-" + System.nanoTime());
        cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        IotProduct p = new IotProduct();
        p.setName("产品-" + System.nanoTime());
        p.setCode("P-" + System.nanoTime());
        p.setCategory(cat);
        p.setNodeType(NodeType.DIRECT.code);
        p.setNetType(NetType.WIFI.code);
        p.setStatus(EnableStatus.ENABLED.code);
        productRepo.save(p);
        return p;
    }
}
