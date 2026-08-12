package xyz.herz.ep.iot.core;

import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.herz.ep.iot.enums.IotDictEnums.*;

/**
 * IoT 通用字典下拉处理器。
 * <p>用法:
 * <pre>
 * &#64;ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class, fetchHandlerParams = "DeviceStatus")
 * private Integer status;
 * </pre>
 * 支持的 key:
 *  DeviceStatus / ThingModelType / DataType / AccessMode / NodeType /
 *  NetType / AlarmLevel / AlarmStatus / EnableStatus
 *
 * <p>同时支持 int code 与 String code 两种枚举(DataType / NetType 为 String code)。
 */
public class IotEnumChoiceFetchHandler implements ChoiceFetchHandler {

    private static final Map<String, Enum<?>[]> LOOKUP = Map.ofEntries(
        Map.entry("DeviceStatus", DeviceStatus.values()),
        Map.entry("ThingModelType", ThingModelType.values()),
        Map.entry("DataType", DataType.values()),
        Map.entry("AccessMode", AccessMode.values()),
        Map.entry("NodeType", NodeType.values()),
        Map.entry("NetType", NetType.values()),
        Map.entry("AlarmLevel", AlarmLevel.values()),
        Map.entry("AlarmStatus", AlarmStatus.values()),
        Map.entry("EnableStatus", EnableStatus.values())
    );

    @Override
    public List<VLModel> fetch(String[] params) {
        List<VLModel> r = new ArrayList<>();
        if (params == null || params.length == 0) return r;
        String key = params[0];
        if (key != null && key.startsWith("{")) {
            int s = key.indexOf("\"key\"");
            if (s < 0) s = key.indexOf("\"field\"");
            if (s > 0) {
                int c1 = key.indexOf(':', s);
                int q1 = key.indexOf('"', c1);
                int q2 = key.indexOf('"', q1 + 1);
                if (q1 > 0 && q2 > q1) key = key.substring(q1 + 1, q2);
            }
        }
        Enum<?>[] enums = LOOKUP.get(key);
        if (enums == null) return r;
        try {
            for (Enum<?> e : enums) {
                Object codeVal = e.getClass().getField("code").get(e);
                String label = (String) e.getClass().getField("label").get(e);
                r.add(new VLModel(String.valueOf(codeVal), label));
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("IoT enum missing code/label: " + key, roe);
        }
        return r;
    }
}
