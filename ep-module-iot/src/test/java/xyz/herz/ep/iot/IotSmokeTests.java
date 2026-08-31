package xyz.herz.ep.iot;

import xyz.herz.ep.iot.entity.*;
import xyz.herz.ep.iot.enums.IotDictEnums.*;
import xyz.herz.ep.iot.handler.*;
import xyz.herz.ep.iot.jpa.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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

    /** 反射获取 BUTTON Handler Bean。 */
    @Autowired ApplicationContext applicationContext;

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

    // =================== TR-2.1 PASSWORD 掩码注解(RED→GREEN) ===================

    @Test
    void device_secret_password_masked() throws NoSuchFieldException {
        // RED→GREEN: IotDevice.deviceSecret 字段视图+编辑均必须为 PASSWORD 掩码
        java.lang.reflect.Field f = IotDevice.class.getDeclaredField("deviceSecret");
        xyz.erupt.annotation.EruptField ann =
            f.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(ann, "deviceSecret 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.ViewType.PASSWORD,
            ann.views()[0].type(),
            "deviceSecret 视图应为 PASSWORD 掩码,防止设备密钥明文展示");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.PASSWORD,
            ann.edit().type(),
            "deviceSecret 编辑应为 PASSWORD 掩码,表单显示占位符保留原值");
    }

    // =================== TR-4.1 collapseActionButton + TR-4.2 @Power(copy) (RED→GREEN) ===================

    @Test
    void collapse_alarm_and_device_and_copy_product() {
        // TR-4.1 (RED): IotAlarm(3 行按钮) / IotDevice(3 行按钮) 应有 collapseActionButton=true
        xyz.erupt.annotation.Erupt alarmErupt =
            IotAlarm.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertTrue(alarmErupt.layout().collapseActionButton(),
            "IotAlarm(3 行动作按钮:处理/解决/忽略) 应启用 collapseActionButton");

        xyz.erupt.annotation.Erupt deviceErupt =
            IotDevice.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertTrue(deviceErupt.layout().collapseActionButton(),
            "IotDevice(3 行动作按钮:激活/启用/禁用) 应启用 collapseActionButton");

        // TR-4.2 (RED): IotProduct 应启用 copy=true
        assertTrue(
            IotProduct.class.getAnnotation(xyz.erupt.annotation.Erupt.class).power().copy(),
            "IotProduct 高频产品档案 应启用 @Power(copy=true) 一键复制行"
        );

        // TR-4.2 (RED): 后端复制 IotProduct 行为验证
        IotProduct src = basicProduct();
        IotProductCategory cat = catRepo.findById(src.getCategory().getId()).orElseThrow();

        IotProduct cp = new IotProduct();
        cp.setName(src.getName() + "-副本");
        cp.setCode(src.getCode() + "-CP");  // unique 约束
        cp.setCategory(cat);
        cp.setNodeType(src.getNodeType());
        cp.setNetType(src.getNetType());
        cp.setStatus(xyz.herz.ep.iot.enums.IotDictEnums.EnableStatus.ENABLED.code);
        cp.setId(null);
        productRepo.save(cp);

        assertNotNull(cp.getId(), "复制 IotProduct 必须生成新 ID");
        assertNotEquals(src.getId(), cp.getId());
        IotProduct cpDb = productRepo.findById(cp.getId()).orElseThrow();
        assertEquals(cat.getId(), cpDb.getCategory().getId(),
            "复制 IotProduct 应保留产品分类关联");
        assertEquals(src.getNodeType(), cpDb.getNodeType(),
            "复制 IotProduct 应保留节点类型");
        assertEquals(src.getNetType(), cpDb.getNetType(),
            "复制 IotProduct 应保留联网类型");
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

    // =================== TR-6A IoT BUTTON: 告警规则 阈值样例验证 (RED→GREEN) ===================

    @Test
    void iot_alarm_rule_button_validate_and_boundary() throws Exception {
        // ===== (1) 注解断言: IotAlarmRule 应有 BUTTON 辅助字段 validateSample =====
        java.lang.reflect.Field sampleF;
        try {
            sampleF = IotAlarmRule.class.getDeclaredField("validateSample");
        } catch (NoSuchFieldException e) {
            fail("IotAlarmRule 缺少 BUTTON 辅助字段: validateSample（输入样例数值点验证触发规则）");
            return;
        }
        assertNotNull(sampleF.getAnnotation(jakarta.persistence.Transient.class),
            "IotAlarmRule.validateSample 必须 @Transient（仅 BUTTON 输入,不入库）");
        xyz.erupt.annotation.EruptField ann =
            sampleF.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(ann, "validateSample 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.BUTTON, ann.edit().type(),
            "validateSample 编辑 type 应为 EditType.BUTTON（样例值触发阈值命中校验）");

        // ===== (2) Handler 存在性 + exec 签名 =====
        Class<?> handlerCls;
        try {
            handlerCls = Class.forName("xyz.herz.ep.iot.handler.IotAlarmRuleValidateButtonHandler");
        } catch (ClassNotFoundException e) {
            fail("缺少 IoT BUTTON Handler: xyz.herz.ep.iot.handler.IotAlarmRuleValidateButtonHandler");
            return;
        }
        java.lang.reflect.Method exec;
        try {
            exec = handlerCls.getMethod("exec", IotAlarmRule.class, BigDecimal.class);
        } catch (NoSuchMethodException e) {
            fail("IotAlarmRuleValidateButtonHandler 必须暴露 exec(IotAlarmRule rule, BigDecimal sampleValue) -> String");
            return;
        }
        Object handler = applicationContext.getBean(handlerCls);
        assertNotNull(handler);

        // ===== (3) TR-6A.1 正常路径: 阈值型 ruleType=threshold, T=30 =====
        // 命中语义: X >= T + 5（上限越界）→ 命中；否则不命中
        IotProduct p = basicProduct();
        IotAlarmRule rule = new IotAlarmRule();
        rule.setProduct(p);
        rule.setName("温度越上限-" + System.nanoTime());
        rule.setIdentifier("temperature");
        rule.setRuleType("threshold");
        rule.setCondition("{\"op\":\">=\",\"field\":\"temperature\"}");
        rule.setThreshold(new BigDecimal("30"));      // T = 30
        rule.setStatus(EnableStatus.ENABLED.code);
        ruleRepo.save(rule);
        assertNotNull(rule.getId());

        // 35: 35 >= 30+5=35 → 边界命中
        String r35 = (String) exec.invoke(handler, rule, new BigDecimal("35"));
        assertTrue(r35.contains("命中"), () -> "T=30 X=35 (≥T+5) 边界应命中,实际:" + r35);

        // 37: 37 >= 35 → 明显命中
        String r37 = (String) exec.invoke(handler, rule, new BigDecimal("37"));
        assertTrue(r37.contains("命中"), () -> "T=30 X=37 (>T+5) 应命中,实际:" + r37);

        // 28: 28 < 35 → 不命中
        String r28 = (String) exec.invoke(handler, rule, new BigDecimal("28"));
        assertTrue(r28.contains("不命中"), () -> "T=30 X=28 (|Δ|=2 <5) 应不命中,实际:" + r28);

        // 25: 25 < 35 → 不命中（TR 要求）
        String r25 = (String) exec.invoke(handler, rule, new BigDecimal("25"));
        assertTrue(r25.contains("不命中"), () -> "T=30 X=25 (单方向仅判断上限) 应不命中,实际:" + r25);

        // ===== (4) 边界异常 =====
        // sampleValue == null → IAE
        try {
            exec.invoke(handler, rule, (BigDecimal) null);
            fail("sample=null 应抛 IllegalArgumentException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof IllegalArgumentException,
                "sample=null 应抛 IAE, cause=" + (cause == null ? null : cause.getClass().getSimpleName()));
        }

        // threshold == null → ISE
        IotAlarmRule ruleNullT = new IotAlarmRule();
        ruleNullT.setProduct(p);
        ruleNullT.setName("NullThreshold-" + System.nanoTime());
        ruleNullT.setRuleType("threshold");
        ruleNullT.setThreshold(null);
        ruleNullT.setStatus(EnableStatus.ENABLED.code);
        ruleRepo.save(ruleNullT);
        try {
            exec.invoke(handler, ruleNullT, new BigDecimal("40"));
            fail("threshold=null 应抛 IllegalStateException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertNotNull(cause, "threshold=null 必须抛异常");
            assertTrue(cause instanceof IllegalStateException,
                "threshold=null 应抛 ISE, cause=" + cause.getClass().getSimpleName());
        }

        // ruleType != "threshold"(如 state) → UnsupportedOperationException(表示当前未实现该方向,可后续扩展)
        IotAlarmRule ruleState = new IotAlarmRule();
        ruleState.setProduct(p);
        ruleState.setName("状态规则-" + System.nanoTime());
        ruleState.setRuleType("state");
        ruleState.setThreshold(BigDecimal.ZERO);
        ruleState.setStatus(EnableStatus.ENABLED.code);
        ruleRepo.save(ruleState);
        try {
            exec.invoke(handler, ruleState, BigDecimal.ONE);
            fail("ruleType=state 当前未实现,应抛 UnsupportedOperationException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertNotNull(cause, "state 规则必须抛未实现异常");
            assertTrue(cause instanceof UnsupportedOperationException,
                "ruleType=state 应抛 UOE(当前仅实现 threshold), cause="
                    + cause.getClass().getSimpleName());
        }
    }
}
