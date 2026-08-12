package xyz.herz.ep.mp.core;

import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.herz.ep.mp.enums.MpDictEnums.*;

/**
 * 公众号(MP)通用字典下拉处理器。
 * <p>用法:
 * <pre>
 * &#64;ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class, fetchHandlerParams = "EnableStatus")
 * private Integer status;
 * </pre>
 * 支持的 key:
 *  SubscribeStatus / MenuStatus / MaterialType / ReplyType / ReplyContentType /
 *  EnableStatus / MessageType / MessageDirection
 *
 * <p>同时兼容 int code 与 String code 两种枚举字段。
 */
public class MpEnumChoiceFetchHandler implements ChoiceFetchHandler {

    private static final Map<String, Enum<?>[]> LOOKUP = Map.ofEntries(
        Map.entry("SubscribeStatus", SubscribeStatus.values()),
        Map.entry("MenuStatus", MenuStatus.values()),
        Map.entry("MaterialType", MaterialType.values()),
        Map.entry("ReplyType", ReplyType.values()),
        Map.entry("ReplyContentType", ReplyContentType.values()),
        Map.entry("EnableStatus", EnableStatus.values()),
        Map.entry("MessageType", MessageType.values()),
        Map.entry("MessageDirection", MessageDirection.values())
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
                java.lang.reflect.Field codeField = e.getClass().getField("code");
                Object codeVal = codeField.get(e);
                String codeStr = codeVal instanceof Number
                    ? String.valueOf(((Number) codeVal).intValue())
                    : String.valueOf(codeVal);
                String label = (String) e.getClass().getField("label").get(e);
                r.add(new VLModel(codeStr, label));
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("MP enum missing code/label: " + key, roe);
        }
        return r;
    }
}
