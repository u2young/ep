package xyz.herz.ep.iot.handler;

import xyz.erupt.annotation.fun.EruptButtonHandler;
import xyz.herz.ep.iot.entity.IotAlarmRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * IoT 告警规则 BUTTON Handler: 将用户在 BUTTON 辅助字段填写的「样例数值」
 * 与规则阈值 T 比较,给出一次命中/不命中的验证提示。
 *
 * <p>当前实现方向(threshold 上限越界): X ≥ T + {@link #MARGIN} → 命中; 否则 不命中。
 * <p>ruleType != "threshold"(例如 state) 抛出 {@link UnsupportedOperationException},
 * 预留后续扩展方向(状态判定 / 下限越界 / 双向阈值)。
 */
@Component
public class IotAlarmRuleValidateButtonHandler implements EruptButtonHandler<IotAlarmRule> {

    /** 阈值浮动容差: sample - threshold ≥ MARGIN 才视为上限告警命中。 */
    public static final BigDecimal MARGIN = new BigDecimal("5");

    @Override
    public String click(IotAlarmRule rule, String[] params) {
        return exec(rule, rule.getValidateSample());
    }

    /**
     * 纯业务 exec(供测试 + click 调用)。
     *
     * @param rule        告警规则(需已持久化或至少已赋 threshold / ruleType)
     * @param sampleValue 模拟上报的样例值
     * @return 命中 / 不命中 提示,含实际比较的数值
     * @throws IllegalArgumentException rule / sampleValue == null
     * @throws IllegalStateException    threshold == null
     * @throws UnsupportedOperationException ruleType 不是 threshold
     */
    public String exec(IotAlarmRule rule, BigDecimal sampleValue) {
        if (rule == null) throw new IllegalArgumentException("规则不能为空");
        if (sampleValue == null) {
            throw new IllegalArgumentException("样例值 sampleValue 不能为空(请在 BUTTON 输入框填写)");
        }
        String ruleType = rule.getRuleType();
        if (!"threshold".equalsIgnoreCase(ruleType)) {
            throw new UnsupportedOperationException(
                "当前仅实现 threshold 类型规则的命中验证(当前 ruleType=" + ruleType
                    + ")。state / 双向阈值等方向待扩展。");
        }
        BigDecimal threshold = rule.getThreshold();
        if (threshold == null) {
            throw new IllegalStateException("规则 threshold 不能为空,无法做阈值命中判断");
        }
        BigDecimal limit = threshold.add(MARGIN);
        int cmp = sampleValue.compareTo(limit);
        if (cmp >= 0) {
            return "命中: 样例值 X=" + sampleValue.toPlainString()
                + " ≥ 阈值 T=" + threshold.toPlainString()
                + " + " + MARGIN.toPlainString()
                + " = " + limit.toPlainString()
                + " → 触发阈值越上限告警";
        } else {
            return "不命中: 样例值 X=" + sampleValue.toPlainString()
                + " < 阈值 T=" + threshold.toPlainString()
                + " + " + MARGIN.toPlainString()
                + " = " + limit.toPlainString()
                + " → 未达阈值越上限条件";
        }
    }
}
