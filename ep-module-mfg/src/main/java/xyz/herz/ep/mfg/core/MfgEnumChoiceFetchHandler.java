package xyz.herz.ep.mfg.core;

import xyz.erupt.annotation.fun.ChoiceFetchHandler;
import xyz.erupt.annotation.fun.VLModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xyz.herz.ep.mfg.enums.MfgDictEnums.*;

/**
 * 生产制造模块通用字典下拉处理器(复刻 ERP 模板 ErpEnumChoiceFetchHandler)。
 * <p>用法:
 * <pre>
 * &#64;ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class, fetchHandlerParams = "WorkOrderStatus")
 * private Integer status;
 * </pre>
 * 支持的 key:
 *  EnableStatus / WorkOrderStatus / JobCardStatus /
 *  StockEntryType / StockEntryStatus / SubcontractingStatus
 */
public class MfgEnumChoiceFetchHandler implements ChoiceFetchHandler {

    private static final Map<String, Enum<?>[]> LOOKUP = Map.ofEntries(
        Map.entry("EnableStatus", EnableStatus.values()),
        Map.entry("WorkOrderStatus", WorkOrderStatus.values()),
        Map.entry("JobCardStatus", JobCardStatus.values()),
        Map.entry("StockEntryType", StockEntryType.values()),
        Map.entry("StockEntryStatus", StockEntryStatus.values()),
        Map.entry("SubcontractingStatus", SubcontractingStatus.values())
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
                int code = e.getClass().getField("code").getInt(e);
                String label = (String) e.getClass().getField("label").get(e);
                r.add(new VLModel(String.valueOf(code), label));
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("MFG enum missing code/label: " + key, roe);
        }
        return r;
    }
}
