package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.enums.CrmDictEnums.*;
import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Erupt 通用字典下拉处理器。
 *
 * 用法示例:
 * <pre>
 * &#64;ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchParams = "{\"field\":\"clue_follow_status\"}")
 * private Integer status;
 * </pre>
 *
 * 支持的 field 参数(和 crm.md 字段一一对应):
 * <ul>
 *   <li>clue_follow_status        → ClueFollowStatus</li>
 *   <li>clue_transform_status     → ClueTransformStatus</li>
 *   <li>lock_status               → LockStatus</li>
 *   <li>deal_status               → DealStatus</li>
 *   <li>business_end_status       → BusinessEndStatus</li>
 *   <li>follow_biz_type           → FollowBizType</li>
 *   <li>follow_way                → FollowWay</li>
 *   <li>source                    → Source</li>
 *   <li>industry                  → Industry</li>
 *   <li>customer_level            → CustomerLevel</li>
 *   <li>sex                       → Sex</li>
 *   <li>ContractStatus            → ContractStatus(0草稿/1生效/2作废)</li>
 *   <li>ReceivableStatus          → ReceivableStatus(0待回款/1部分回款/2已回款)</li>
 *   <li>TeamRole                  → TeamRole(1负责人/2跟进人/3只读)</li>
 * </ul>
 */
public class CrmEnumChoiceFetchHandler implements ChoiceFetchHandler {

    private static final Map<String, Enum<?>[]> LOOKUP = Map.ofEntries(
        Map.entry("clue_follow_status", ClueFollowStatus.values()),
        Map.entry("clue_transform_status", ClueTransformStatus.values()),
        Map.entry("lock_status", LockStatus.values()),
        Map.entry("deal_status", DealStatus.values()),
        Map.entry("business_end_status", BusinessEndStatus.values()),
        Map.entry("follow_biz_type", FollowBizType.values()),
        Map.entry("follow_way", FollowWay.values()),
        Map.entry("source", Source.values()),
        Map.entry("industry", Industry.values()),
        Map.entry("customer_level", CustomerLevel.values()),
        Map.entry("sex", Sex.values()),
        Map.entry("ContractStatus", ContractStatus.values()),
        Map.entry("ReceivableStatus", ReceivableStatus.values()),
        Map.entry("TeamRole", TeamRole.values())
    );

    @Override
    public List<VLModel> fetch(String[] params) {
        List<VLModel> result = new ArrayList<>();
        if (params == null || params.length == 0) return result;
        String field = parseField(params[0]);
        Enum<?>[] enums = LOOKUP.get(field);
        if (enums == null) return result;
        try {
            for (Enum<?> e : enums) {
                int code = e.getClass().getField("code").getInt(e);
                String label = (String) e.getClass().getField("label").get(e);
                result.add(new VLModel(String.valueOf(code), label));
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("Crm enum missing code/label field: " + field, roe);
        }
        return result;
    }

    private String parseField(String p) {
        // params 既可以直接写 "clue_follow_status",也可以写完整 JSON "{\"field\":\"clue_follow_status\"}"
        if (p.startsWith("{")) {
            try {
                int s = p.indexOf("\"field\"");
                if (s < 0) return p;
                int c1 = p.indexOf(':', s);
                int q1 = p.indexOf('"', c1);
                int q2 = p.indexOf('"', q1 + 1);
                return p.substring(q1 + 1, q2);
            } catch (Exception ignore) {}
        }
        return p;
    }
}
