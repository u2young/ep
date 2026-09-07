package xyz.herz.ep.ast.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;
import xyz.herz.ep.ast.entity.depreciation.AstDepreciationSchedule;
import xyz.herz.ep.ast.enums.AstDictEnums.AssetStatus;
import xyz.herz.ep.ast.enums.AstDictEnums.DepreciationMethod;
import xyz.herz.ep.ast.jpa.depreciation.AstDepreciationScheduleRepository;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产折旧计算引擎(参考 ERPNext Asset Depreciation)。
 * <p>支持三种折旧方法:
 * <ul>
 *   <li>直线法 STRAIGHT_LINE:(原值-残值)/使用年限(月),每期等额,最后一期取剩余。</li>
 *   <li>余额递减 DECLINING_BALANCE:当前净值 × 月折旧率(年率/12/100)。</li>
 *   <li>双倍余额递减 DOUBLE_DECLINING:当前净值 × 2/使用年限(月)。</li>
 * </ul>
 *
 * <p>GL 过账(T-E3):每期折旧生成一条凭证,sourceType=ASSET_DEPRECIATION,sourceId=schedule.id;
 * 借:折旧费用科目;贷:累计折旧科目。若 FinPostingFacade 未注入(fin 模块不在 classpath)则跳过过账。
 */
@Service
public class AstDepreciationCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal TWO = new BigDecimal("2");

    private final AstDepreciationScheduleRepository scheduleRepo;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    public AstDepreciationCalculator(AstDepreciationScheduleRepository scheduleRepo) {
        this.scheduleRepo = scheduleRepo;
    }

    /** 计算下一期折旧金额(只读,不修改实体)。 */
    public BigDecimal computePeriodAmount(AstAsset asset) {
        BigDecimal base = nz(asset.getPurchaseAmount());
        BigDecimal salvage = nz(asset.getSalvageValue());
        int method = asset.getDepreciationMethod() != null
            ? asset.getDepreciationMethod() : DepreciationMethod.STRAIGHT_LINE.code;
        int periods = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths() : 0;
        BigDecimal currentVal = asset.getCurrentValue() != null ? asset.getCurrentValue() : base;
        BigDecimal totalDep = nz(asset.getTotalDepreciation());

        if (method == DepreciationMethod.MANUAL.code) return BigDecimal.ZERO;
        if (periods <= 0 && method != DepreciationMethod.DECLINING_BALANCE.code) return BigDecimal.ZERO;

        BigDecimal depreciableBase = base.subtract(salvage);
        if (depreciableBase.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal amount;
        if (method == DepreciationMethod.STRAIGHT_LINE.code) {
            BigDecimal perPeriod = depreciableBase.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
            BigDecimal remaining = depreciableBase.subtract(totalDep);
            amount = perPeriod.compareTo(remaining) > 0 ? remaining : perPeriod;
        } else if (method == DepreciationMethod.DECLINING_BALANCE.code) {
            BigDecimal rate = asset.getDepreciationRate();
            if (rate == null || rate.signum() <= 0) return BigDecimal.ZERO;
            BigDecimal monthlyRate = rate.divide(HUNDRED, 8, RoundingMode.HALF_UP)
                .divide(TWELVE, 8, RoundingMode.HALF_UP);
            amount = currentVal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal floor = currentVal.subtract(salvage);
            if (amount.compareTo(floor) > 0) amount = floor;
        } else if (method == DepreciationMethod.DOUBLE_DECLINING.code) {
            BigDecimal monthlyRate = TWO.divide(BigDecimal.valueOf(periods), 8, RoundingMode.HALF_UP);
            amount = currentVal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal floor = currentVal.subtract(salvage);
            if (amount.compareTo(floor) > 0) amount = floor;
        } else {
            return BigDecimal.ZERO;
        }
        return amount.signum() < 0 ? BigDecimal.ZERO : amount;
    }

    /**
     * 计提一期折旧:创建 schedule 记录 + 更新 asset 字段 + GL 过账。
     * @return 持久化后的折旧计划记录(含 journalEntryId 若已过账)
     */
    public AstDepreciationSchedule applyDepreciation(AstAsset asset, LocalDate depDate) {
        int st = asset.getStatus() != null ? asset.getStatus() : -1;
        if (st != AssetStatus.AVAILABLE.code && st != AssetStatus.PARTIALLY_DEPRECIATED.code) {
            throw new IllegalStateException("只有可用/部分折旧状态可计提折旧,当前状态=" + st);
        }
        int periods = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths() : 0;
        int done = asset.getDepreciatedPeriods() != null ? asset.getDepreciatedPeriods() : 0;
        if (periods > 0 && done >= periods) {
            throw new IllegalStateException("已折旧期数 " + done + " >= 使用年限 " + periods + ",不可继续折旧");
        }

        BigDecimal base = nz(asset.getPurchaseAmount());
        BigDecimal salvage = nz(asset.getSalvageValue());
        BigDecimal totalDep = nz(asset.getTotalDepreciation());
        BigDecimal amount = computePeriodAmount(asset);
        // 最后一期补齐:累计折旧 = 可折旧基数(消除每期 833.33×12=9999.96 的截断误差)
        if (periods > 0 && done + 1 >= periods) {
            BigDecimal depreciableBase = base.subtract(salvage);
            BigDecimal remaining = depreciableBase.subtract(totalDep);
            if (remaining.signum() > 0) {
                amount = remaining;
            }
        }
        if (amount.signum() <= 0) {
            throw new IllegalStateException("折旧金额为 0,检查折旧方法/参数/残值");
        }
        BigDecimal newTotal = totalDep.add(amount);
        BigDecimal newCurrent = base.subtract(newTotal);

        AstDepreciationSchedule schedule = new AstDepreciationSchedule();
        schedule.setAsset(asset);
        schedule.setPeriodNo(done + 1);
        schedule.setDepreciationDate(depDate);
        schedule.setDepreciationAmount(amount);
        schedule.setAccumulatedDepreciation(newTotal);
        schedule.setNetBookValue(newCurrent);
        schedule.setPosted(false);
        schedule = scheduleRepo.save(schedule);

        // GL 过账(若 fin 在 classpath 且科目配置齐全)
        Long journalId = postDepreciationGl(asset, schedule, amount, depDate);
        if (journalId != null) {
            schedule.setPosted(true);
            schedule.setJournalEntryId(journalId);
            scheduleRepo.save(schedule);
            asset.setLastJournalId(journalId);
        }

        // 更新资产字段
        asset.setTotalDepreciation(newTotal);
        asset.setCurrentValue(newCurrent);
        asset.setDepreciatedPeriods(done + 1);

        // 状态转移:折完或净值≤残值 → 完全折旧
        BigDecimal depreciableBase = base.subtract(salvage);
        boolean fullyDepreciated = (periods > 0 && done + 1 >= periods)
            || newCurrent.compareTo(salvage) <= 0
            || depreciableBase.signum() <= 0;
        asset.setStatus(fullyDepreciated
            ? AssetStatus.FULLY_DEPRECIATED.code
            : AssetStatus.PARTIALLY_DEPRECIATED.code);

        return schedule;
    }

    private Long postDepreciationGl(AstAsset asset, AstDepreciationSchedule schedule,
                                    BigDecimal amount, LocalDate depDate) {
        if (postingFacade == null) return null;
        AstAssetCategory cat = asset.getCategory();
        if (cat == null) return null;
        String depExpense = cat.getDepreciationExpenseAccountCode();
        String accDep = cat.getAccumulatedDepreciationAccountCode();
        if (depExpense == null || accDep == null) return null;

        FinPostingFacade.PostingRequest req = new FinPostingFacade.PostingRequest(
            JournalSourceType.ASSET_DEPRECIATION.code,
            schedule.getId(),
            asset.getAssetNo() + "-DEP-" + schedule.getPeriodNo(),
            depDate,
            List.of(
                new FinPostingFacade.PostingLine(depExpense, amount, null,
                    null, null, null, asset.getCostCenterId(),
                    "资产折旧 " + asset.getAssetNo() + " 期 " + schedule.getPeriodNo()),
                new FinPostingFacade.PostingLine(accDep, null, amount,
                    null, null, null, asset.getCostCenterId(),
                    "累计折旧 " + asset.getAssetNo() + " 期 " + schedule.getPeriodNo())
            ),
            "资产折旧 " + asset.getAssetNo() + " 期 " + schedule.getPeriodNo()
        );
        return postingFacade.post(req);
    }

    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
}
