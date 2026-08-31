package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.crm.entity.CrmReceivablePlan;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmReceivablePlanRepository;
import xyz.erupt.annotation.fun.EruptButtonHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 合同 BUTTON Handler:按「期数 / 首期日期 / 间隔月」均分合同金额,
 * 自动生成 N 条 {@link CrmReceivablePlan} 回款计划并持久化。
 *
 * <p>通过 {@code @ButtonType(handler = CrmContractAutoPlanButtonHandler.class)}
 * 绑定到 {@link CrmContract} 的 3 个 EditType.BUTTON 辅助输入字段。
 */
@Component
public class CrmContractAutoPlanButtonHandler implements EruptButtonHandler<CrmContract> {

    @Autowired CrmReceivablePlanRepository planRepo;

    /**
     * Erupt 表单按钮点击入口: BUTTON 字段输入会自动被绑定到 contract 辅助字段,
     * 委托给业务方法 {@link #exec(Integer, LocalDate, Integer, CrmContract)}。
     */
    @Override
    @Transactional
    public String click(CrmContract contract, String[] params) {
        return exec(
            contract.getPlanPeriods(),
            contract.getPlanStartDate(),
            contract.getPlanIntervalMonths(),
            contract
        );
    }

    /**
     * 纯业务 exec 方法(供单元测试 + {@link #click} 直接调用)。
     *
     * @param periods        分几期,必须 ≥ 1
     * @param startDate      首期计划回款日,非空
     * @param intervalMonths 相邻两期间隔月数,必须 ≥ 1
     * @param contract       目标合同,必须已持久化(id 非空)且 amount 不为空
     * @return 成功提示,包含实际生成的条数
     * @throws IllegalArgumentException periods / interval ≤ 0、startDate / contract 为空、contract.id 为空
     * @throws IllegalStateException    contract.amount 为空或 ≤ 0
     */
    public String exec(Integer periods, LocalDate startDate, Integer intervalMonths, CrmContract contract) {
        if (contract == null) {
            throw new IllegalArgumentException("合同对象不能为空");
        }
        if (periods == null || periods <= 0) {
            throw new IllegalArgumentException("期数必须 > 0(实际 periods=" + periods + ")");
        }
        if (intervalMonths == null || intervalMonths <= 0) {
            throw new IllegalArgumentException("间隔月数必须 > 0(实际 interval=" + intervalMonths + ")");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("首期计划回款日期不能为空");
        }
        if (contract.getId() == null) {
            throw new IllegalArgumentException("合同必须先保存才能生成回款计划(contract.id 为空)");
        }
        BigDecimal amount = contract.getAmount();
        if (amount == null) {
            throw new IllegalStateException("合同金额 amount 不能为空,无法分期生成回款计划");
        }
        if (BigDecimal.ZERO.compareTo(amount) >= 0) {
            throw new IllegalStateException("合同金额必须 > 0,无法生成回款计划(amount=" + amount + ")");
        }

        BigDecimal perAmount = amount.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        List<CrmReceivablePlan> plans = new ArrayList<>(periods);
        for (int i = 0; i < periods; i++) {
            CrmReceivablePlan p = new CrmReceivablePlan();
            p.setContract(contract);
            p.setPeriodNo(i + 1);
            p.setPlanAmount(perAmount);
            p.setPlanDate(startDate.plusMonths((long) i * intervalMonths));
            p.setReceivedAmount(BigDecimal.ZERO);
            p.setStatus(CrmDictEnums.ReceivableStatus.PENDING.code);
            plans.add(p);
        }
        planRepo.saveAll(plans);
        return "成功生成 " + plans.size() + " 条回款计划";
    }
}
