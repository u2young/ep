package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmReceivablePlan;
import xyz.herz.ep.crm.entity.CrmReceivableRecord;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmReceivablePlanRepository;
import xyz.herz.ep.crm.jpa.CrmReceivableRecordRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回款记录【确认回款】按钮:把 record.amount 累加到关联 plan.receivedAmount,
 * 并根据累计金额刷新 plan.status(部分回款 / 已回款)。
 *
 * <p>规则:
 * <ul>
 *   <li>record.plan 为空:跳过计划更新,只保留 record 本身。</li>
 *   <li>累加后 receivedAmount &lt; planAmount  → status=1 部分回款</li>
 *   <li>累加后 receivedAmount ≥ planAmount  → status=2 已回款</li>
 *   <li>已回款(2)的计划再次确认:报错,防止超额回款</li>
 * </ul>
 *
 * <p>强校验:record.plan 必须非空且状态非「已回款」才允许确认。
 */
@Component
public class CrmReceivableConfirmHandler implements OperationHandler<CrmReceivableRecord, String> {

    @Resource private CrmReceivableRecordRepository recordRepo;
    @Resource private CrmReceivablePlanRepository planRepo;

    @Override
    @Transactional
    public String exec(List<CrmReceivableRecord> data, String param, String[] eruptParams) {
        int ok = 0;
        StringBuilder sb = new StringBuilder();
        for (CrmReceivableRecord r : data) {
            try {
                CrmReceivablePlan plan = r.getPlan();
                if (plan == null) {
                    sb.append("回款记录[id=").append(r.getId()).append("] 未关联回款计划,跳过计划更新\n");
                    recordRepo.save(r);
                    ok++;
                    continue;
                }
                Integer curStatus = plan.getStatus() == null ? 0 : plan.getStatus();
                if (curStatus == CrmDictEnums.ReceivableStatus.RECEIVED.code) {
                    sb.append("回款计划[id=").append(plan.getId()).append("] 已回款,拒绝超额确认\n");
                    continue;
                }
                BigDecimal add = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
                BigDecimal cur = plan.getReceivedAmount() == null ? BigDecimal.ZERO : plan.getReceivedAmount();
                BigDecimal total = cur.add(add);
                plan.setReceivedAmount(total);

                BigDecimal planAmt = plan.getPlanAmount() == null ? BigDecimal.ZERO : plan.getPlanAmount();
                if (total.compareTo(planAmt) >= 0) {
                    plan.setStatus(CrmDictEnums.ReceivableStatus.RECEIVED.code);
                } else if (total.compareTo(BigDecimal.ZERO) > 0) {
                    plan.setStatus(CrmDictEnums.ReceivableStatus.PARTIAL.code);
                } else {
                    plan.setStatus(CrmDictEnums.ReceivableStatus.PENDING.code);
                }
                planRepo.save(plan);
                recordRepo.save(r);
                ok++;
            } catch (Exception e) {
                sb.append("回款记录[id=").append(r == null ? "-" : r.getId())
                    .append("] 确认失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "确认回款:成功 " + ok + " 条。\n" + sb;
    }
}
