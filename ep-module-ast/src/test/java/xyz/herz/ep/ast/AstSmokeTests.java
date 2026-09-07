package xyz.herz.ep.ast;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.ast.core.AstDepreciationCalculator;
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;
import xyz.herz.ep.ast.entity.depreciation.AstDepreciationSchedule;
import xyz.herz.ep.ast.entity.location.AstLocation;
import xyz.herz.ep.ast.entity.movement.AstAssetMovement;
import xyz.herz.ep.ast.entity.repair.AstAssetRepair;
import xyz.herz.ep.ast.enums.AstDictEnums.AssetStatus;
import xyz.herz.ep.ast.enums.AstDictEnums.DepreciationMethod;
import xyz.herz.ep.ast.enums.AstDictEnums.MovementStatus;
import xyz.herz.ep.ast.enums.AstDictEnums.MovementType;
import xyz.herz.ep.ast.enums.AstDictEnums.RepairStatus;
import xyz.herz.ep.ast.handler.asset.AstAssetLifecycleHandler;
import xyz.herz.ep.ast.handler.movement.AstMovementLifecycleHandler;
import xyz.herz.ep.ast.handler.repair.AstRepairLifecycleHandler;
import xyz.herz.ep.ast.jpa.asset.AstAssetRepository;
import xyz.herz.ep.ast.jpa.category.AstAssetCategoryRepository;
import xyz.herz.ep.ast.jpa.depreciation.AstDepreciationScheduleRepository;
import xyz.herz.ep.ast.jpa.location.AstLocationRepository;
import xyz.herz.ep.ast.jpa.movement.AstAssetMovementRepository;
import xyz.herz.ep.ast.jpa.repair.AstAssetRepairRepository;
import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.AccountType;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.jpa.account.FinAccountRepository;
import xyz.herz.ep.fin.jpa.journal.FinJournalEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资产管理模块冒烟测试(参考 ERPNext Asset DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-E 验收矩阵:
 * <ol>
 *   <li>Ast1 直线法全生命周期:提交→12期折旧→完全折旧 + GL 过账 ASSET_DEPRECIATION(借费用/贷累计折旧)</li>
 *   <li>Ast2 余额递减法:每期折旧递减 + 累计折旧/净值精确性</li>
 *   <li>Ast3 双倍余额递减法:计算精确 + 最后一期 floor 保护(净值不跌破残值)</li>
 *   <li>Ast4 出售/报废处置:GL 过账 ASSET_DISPOSAL(借累计折旧+净值费用/贷固定资产)+ 状态终态</li>
 *   <li>Ast5 资产转移单同步 + 维修单状态机:转移提交同步 custodian/costCenter,维修完成/取消</li>
 *   <li>Ast6 状态机守卫:草稿不可折旧/已出售不可再处置/状态字段禁止表单直改</li>
 * </ol>
 *
 * <p>会计科目用 category 上的 accountCode 快照(FA/ADA/DEP),种 3 个 FinAccount 让 GL 过账生效;
 * 保管人/成本中心用快照 name/id,不 REF UPMS,保持 ast 独立可测。
 * 折旧计算委托 {@link AstDepreciationCalculator};GL 过账通过 {@link FinPostingFacade} 落地 fin 凭证。
 */
@SpringBootTest(classes = AstTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class AstSmokeTests {

    @Autowired AstAssetRepository assetRepo;
    @Autowired AstAssetCategoryRepository categoryRepo;
    @Autowired AstLocationRepository locationRepo;
    @Autowired AstDepreciationScheduleRepository scheduleRepo;
    @Autowired AstAssetMovementRepository movementRepo;
    @Autowired AstAssetRepairRepository repairRepo;
    @Autowired AstAssetLifecycleHandler assetLifecycle;
    @Autowired AstMovementLifecycleHandler movementLifecycle;
    @Autowired AstRepairLifecycleHandler repairLifecycle;
    @Autowired AstDepreciationCalculator depreciationCalculator;
    @Autowired FinAccountRepository accountRepo;
    @Autowired FinJournalEntryRepository journalRepo;
    @Autowired FinPostingFacade postingFacade;

    private static final String FA = "FA";   // 固定资产科目
    private static final String ADA = "ADA"; // 累计折旧科目
    private static final String DEP = "DEP"; // 折旧费用科目

    /** 种 3 个 GL 科目(固定资产/累计折旧/折旧费用),让折旧与处置过账生效。 */
    private void seedGlAccounts() {
        accountRepo.save(buildAccount(FA, "固定资产", AccountType.ASSET));
        accountRepo.save(buildAccount(ADA, "累计折旧", AccountType.ASSET));
        accountRepo.save(buildAccount(DEP, "折旧费用", AccountType.EXPENSE));
    }

    private FinAccount buildAccount(String code, String name, AccountType type) {
        FinAccount a = new FinAccount();
        a.setCode(code);
        a.setName(name);
        a.setAccountType(type.code);
        a.setIsGroup(false);
        a.setStatus(EnableStatus.ENABLED.code);
        return a;
    }

    /** 种资产类别,挂三个科目 code 快照 + 直线法。 */
    private AstAssetCategory seedCategory(DepreciationMethod method, BigDecimal rate, Integer lifeMonths) {
        AstAssetCategory cat = new AstAssetCategory();
        cat.setCode("CAT-" + System.nanoTime());
        cat.setName("测试类别-" + method.label);
        cat.setDepreciationMethod(method.code);
        cat.setDepreciationRate(rate);
        cat.setUsefulLifeMonths(lifeMonths);
        cat.setFixedAssetAccountCode(FA);
        cat.setAccumulatedDepreciationAccountCode(ADA);
        cat.setDepreciationExpenseAccountCode(DEP);
        cat.setStatus(EnableStatus.ENABLED.code);
        return categoryRepo.save(cat);
    }

    private AstAsset buildAsset(String no, AstAssetCategory cat, BigDecimal purchase,
                               BigDecimal salvage, Integer lifeMonths, DepreciationMethod method) {
        AstAsset a = new AstAsset();
        a.setAssetNo(no);
        a.setName("测试资产-" + no);
        a.setCategory(cat);
        a.setStatus(AssetStatus.DRAFT.code);
        a.setPurchaseDate(LocalDate.of(2026, 1, 1));
        a.setPurchaseAmount(purchase);
        a.setSalvageValue(salvage);
        a.setUsefulLifeMonths(lifeMonths);
        a.setDepreciationMethod(method.code);
        if (method == DepreciationMethod.DECLINING_BALANCE) {
            a.setDepreciationRate(new BigDecimal("24"));
        }
        a.setCustodianName("张三");
        a.setCostCenterId(5001L);
        return assetRepo.save(a);
    }

    /** 提交资产:草稿 → 可用,初始化净值=原值。 */
    private AstAsset commitAsset(AstAsset a) {
        String r = assetLifecycle.exec(List.of(a), null,
            new String[]{AstAssetLifecycleHandler.CODE_COMMIT});
        assertTrue(r.contains("成功 1"), "提交应成功,实际=" + r);
        return assetRepo.findById(a.getId()).orElseThrow();
    }

    /** 计提一期折旧并返回刷新后的资产。 */
    private AstAsset depreciateOnce(Long assetId) {
        AstAsset current = assetRepo.findById(assetId).orElseThrow();
        String r = assetLifecycle.exec(List.of(current), null,
            new String[]{AstAssetLifecycleHandler.CODE_DEPRECIATE});
        assertTrue(r.contains("成功 1"), "折旧应成功,实际=" + r);
        return assetRepo.findById(assetId).orElseThrow();
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    // =================== (1) 直线法全生命周期 + GL 过账 ===================
    @Test
    void ast1_straight_line_full_lifecycle_and_gl() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.STRAIGHT_LINE, null, 12);
        // 原值 12000 / 残值 2000 / 12 期 → 可折旧基数 10000,每期 833.33,末期补齐至 10000
        AstAsset asset = buildAsset("AST-T-001", cat, bd("12000"), bd("2000"), 12,
            DepreciationMethod.STRAIGHT_LINE);
        asset = commitAsset(asset);
        assertEquals(AssetStatus.AVAILABLE.code, asset.getStatus());
        assertEquals(0, asset.getCurrentValue().compareTo(bd("12000")), "提交后净值=原值 12000");
        assertEquals(0, asset.getTotalDepreciation().compareTo(BigDecimal.ZERO),
            "提交后累计折旧=0(applyCommit 初始化)");

        // 折 12 期至完全折旧
        for (int i = 0; i < 12; i++) {
            asset = depreciateOnce(asset.getId());
        }
        assertEquals(AssetStatus.FULLY_DEPRECIATED.code, asset.getStatus(), "12 期后应完全折旧");
        assertEquals(0, asset.getTotalDepreciation().compareTo(bd("10000.00")),
            "累计折旧精确=可折旧基数 10000(末期补齐消除截断误差)");
        assertEquals(0, asset.getCurrentValue().compareTo(bd("2000.00")), "净值=残值 2000");
        assertEquals(12, asset.getDepreciatedPeriods(), "已折旧期数=12");

        // 子表:12 条折旧计划,全部已过账
        List<AstDepreciationSchedule> schedules = scheduleRepo.findByAsset_Id(asset.getId());
        assertEquals(12, schedules.size(), "应生成 12 条折旧计划");
        AstDepreciationSchedule last = schedules.get(schedules.size() - 1);
        assertTrue(last.getPosted(), "最后一期应已过账");
        assertNotNull(last.getJournalEntryId(), "应回写凭证 ID");
        assertEquals(0, last.getAccumulatedDepreciation().compareTo(bd("10000.00")),
            "末期累计折旧=10000");
        assertEquals(0, last.getNetBookValue().compareTo(bd("2000.00")), "末期净值=2000");

        // GL 验证:借 DEP / 贷 ADA,sourceType=ASSET_DEPRECIATION
        List<FinJournalEntry> journals = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.ASSET_DEPRECIATION.code, last.getId());
        assertEquals(1, journals.size(), "末期应生成 1 张折旧凭证");
        FinJournalEntry je = journals.get(0);
        assertEquals(0, je.getTotalDebit().compareTo(last.getDepreciationAmount()), "借方=本期折旧额");
        assertEquals(0, je.getTotalCredit().compareTo(last.getDepreciationAmount()), "贷方=本期折旧额");
        assertTrue(je.getItems().stream().anyMatch(i ->
            DEP.equals(i.getAccount().getCode()) && i.getDebit().signum() > 0), "凭证应含 DEP 借方行");
        assertTrue(je.getItems().stream().anyMatch(i ->
            ADA.equals(i.getAccount().getCode()) && i.getCredit().signum() > 0), "凭证应含 ADA 贷方行");
        assertEquals(asset.getLastJournalId(), last.getJournalEntryId(), "资产 lastJournalId 应同步");
    }

    // =================== (2) 余额递减法:每期递减 + 累计精确 ===================
    @Test
    void ast2_declining_balance_depreciation() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.DECLINING_BALANCE, bd("24"), 12);
        // 原值 10000 / 残值 0 / 年率 24% → 月率 0.02,每期 = 净值 × 0.02
        AstAsset asset = buildAsset("AST-T-002", cat, bd("10000"), bd("0"), 12,
            DepreciationMethod.DECLINING_BALANCE);
        asset = commitAsset(asset);

        // 第1期 200.00 / 第2期 196.00 / 第3期 192.08(递减)
        AstAsset a1 = depreciateOnce(asset.getId());
        assertEquals(0, a1.getTotalDepreciation().compareTo(bd("200.00")), "第1期累计=200");
        assertEquals(0, a1.getCurrentValue().compareTo(bd("9800.00")), "第1期净值=9800");
        assertEquals(AssetStatus.PARTIALLY_DEPRECIATED.code, a1.getStatus());

        AstAsset a2 = depreciateOnce(asset.getId());
        assertEquals(0, a2.getTotalDepreciation().compareTo(bd("396.00")), "第2期累计=200+196=396");
        assertEquals(0, a2.getCurrentValue().compareTo(bd("9604.00")), "第2期净值=9604");

        AstAsset a3 = depreciateOnce(asset.getId());
        assertEquals(0, a3.getTotalDepreciation().compareTo(bd("588.08")), "第3期累计=396+192.08=588.08");
        assertEquals(0, a3.getCurrentValue().compareTo(bd("9411.92")), "第3期净值=9411.92");

        // 递减断言:200 > 196 > 192.08
        List<AstDepreciationSchedule> schedules = scheduleRepo.findByAsset_Id(asset.getId());
        assertEquals(3, schedules.size());
        BigDecimal d1 = schedules.get(0).getDepreciationAmount();
        BigDecimal d2 = schedules.get(1).getDepreciationAmount();
        BigDecimal d3 = schedules.get(2).getDepreciationAmount();
        assertTrue(d1.compareTo(d2) > 0 && d2.compareTo(d3) > 0,
            "余额递减法每期折旧应递减: " + d1 + " > " + d2 + " > " + d3);
        assertEquals(AssetStatus.PARTIALLY_DEPRECIATED.code, a3.getStatus(), "未折完应保持部分折旧");
    }

    // =================== (3) 双倍余额递减法:计算精确 + floor 保护 ===================
    @Test
    void ast3_double_declining_and_floor_guard() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.DOUBLE_DECLINING, null, 12);
        // 原值 12000 / 残值 2000 / 12 期 → 月率 2/12,每期 = 净值 × 2/12
        AstAsset asset = buildAsset("AST-T-003", cat, bd("12000"), bd("2000"), 12,
            DepreciationMethod.DOUBLE_DECLINING);
        asset = commitAsset(asset);

        // 第1期 12000×2/12=2000.00 / 第2期 10000×2/12=1666.67
        AstAsset a1 = depreciateOnce(asset.getId());
        assertEquals(0, a1.getTotalDepreciation().compareTo(bd("2000.00")), "第1期累计=2000");
        assertEquals(0, a1.getCurrentValue().compareTo(bd("10000.00")), "第1期净值=10000");

        AstAsset a2 = depreciateOnce(asset.getId());
        assertEquals(0, a2.getTotalDepreciation().compareTo(bd("3666.67")), "第2期累计=2000+1666.67");
        assertEquals(0, a2.getCurrentValue().compareTo(bd("8333.33")), "第2期净值=8333.33");

        // floor 保护:构造净值接近残值的资产,折旧额不得跌破 净值-残值
        // currentValue=2100 / salvage=2000 → 原始计算 2100×2/12=350,但 floor=100,故 amount=100
        AstAsset nearFloor = new AstAsset();
        nearFloor.setPurchaseAmount(bd("12000"));
        nearFloor.setCurrentValue(bd("2100"));
        nearFloor.setSalvageValue(bd("2000"));
        nearFloor.setUsefulLifeMonths(12);
        nearFloor.setDepreciationMethod(DepreciationMethod.DOUBLE_DECLINING.code);
        nearFloor.setTotalDepreciation(bd("9900"));
        BigDecimal floorAmount = depreciationCalculator.computePeriodAmount(nearFloor);
        assertEquals(0, floorAmount.compareTo(bd("100.00")),
            "净值 2100 残值 2000 时,折旧应被 floor 限制为 100,实际=" + floorAmount);
    }

    // =================== (4) 出售/报废处置 + GL 过账 ASSET_DISPOSAL ===================
    @Test
    void ast4_sell_and_scrap_disposal_gl() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.STRAIGHT_LINE, null, 12);

        // 资产 A:原值 10000 / 残值 0,提交 + 折1期(累计 833.33 / 净值 9166.67) → 出售
        AstAsset assetA = buildAsset("AST-T-004A", cat, bd("10000"), bd("0"), 12,
            DepreciationMethod.STRAIGHT_LINE);
        assetA = commitAsset(assetA);
        assetA = depreciateOnce(assetA.getId());
        assertEquals(0, assetA.getTotalDepreciation().compareTo(bd("833.33")), "折1期累计=833.33");

        String sellResult = assetLifecycle.exec(List.of(assetA), null,
            new String[]{AstAssetLifecycleHandler.CODE_SELL});
        assertTrue(sellResult.contains("成功 1"), "出售应成功,实际=" + sellResult);
        AstAsset sold = assetRepo.findById(assetA.getId()).orElseThrow();
        assertEquals(AssetStatus.SOLD.code, sold.getStatus(), "出售后状态=SOLD");
        assertNotNull(sold.getLastJournalId(), "出售应回写 lastJournalId");

        // 处置 GL:借 ADA 833.33 + 借 DEP 9166.67 = 贷 FA 10000
        List<FinJournalEntry> disposalA = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.ASSET_DISPOSAL.code, assetA.getId());
        assertEquals(1, disposalA.size(), "出售应生成 1 张处置凭证");
        FinJournalEntry jeA = disposalA.get(0);
        assertEquals(0, jeA.getTotalDebit().compareTo(bd("10000.00")), "借方合计=10000");
        assertEquals(0, jeA.getTotalCredit().compareTo(bd("10000.00")), "贷方合计=10000");
        assertTrue(jeA.getItems().stream().anyMatch(i ->
            ADA.equals(i.getAccount().getCode()) && i.getDebit().compareTo(bd("833.33")) == 0),
            "凭证应含 ADA 借方 833.33(转出累计折旧)");
        assertTrue(jeA.getItems().stream().anyMatch(i ->
            DEP.equals(i.getAccount().getCode()) && i.getDebit().compareTo(bd("9166.67")) == 0),
            "凭证应含 DEP 借方 9166.67(净值转出)");
        assertTrue(jeA.getItems().stream().anyMatch(i ->
            FA.equals(i.getAccount().getCode()) && i.getCredit().compareTo(bd("10000.00")) == 0),
            "凭证应含 FA 贷方 10000(转出固定资产)");

        // 资产 B:原值 5000 / 残值 0,仅提交(无折旧) → 报废
        AstAsset assetB = buildAsset("AST-T-004B", cat, bd("5000"), bd("0"), 12,
            DepreciationMethod.STRAIGHT_LINE);
        assetB = commitAsset(assetB);

        String scrapResult = assetLifecycle.exec(List.of(assetB), null,
            new String[]{AstAssetLifecycleHandler.CODE_SCRAP});
        assertTrue(scrapResult.contains("成功 1"), "报废应成功,实际=" + scrapResult);
        AstAsset scrapped = assetRepo.findById(assetB.getId()).orElseThrow();
        assertEquals(AssetStatus.SCRAPPED.code, scrapped.getStatus(), "报废后状态=SCRAPPED");

        // 处置 GL(无累计折旧):借 DEP 5000 = 贷 FA 5000
        List<FinJournalEntry> disposalB = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.ASSET_DISPOSAL.code, assetB.getId());
        assertEquals(1, disposalB.size(), "报废应生成 1 张处置凭证");
        FinJournalEntry jeB = disposalB.get(0);
        assertEquals(0, jeB.getTotalDebit().compareTo(bd("5000.00")), "借方合计=5000");
        assertEquals(0, jeB.getTotalCredit().compareTo(bd("5000.00")), "贷方合计=5000");
        assertTrue(jeB.getItems().stream().anyMatch(i ->
            FA.equals(i.getAccount().getCode()) && i.getCredit().compareTo(bd("5000.00")) == 0),
            "报废凭证应含 FA 贷方 5000");
        assertTrue(jeB.getItems().stream().noneMatch(i ->
            ADA.equals(i.getAccount().getCode())), "无累计折旧时不应有 ADA 行");
    }

    // =================== (5) 转移单同步资产 + 维修单状态机 ===================
    @Test
    void ast5_movement_sync_and_repair_lifecycle() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.STRAIGHT_LINE, null, 12);
        AstLocation loc = new AstLocation();
        loc.setCode("LOC-A");
        loc.setName("仓库A");
        loc.setStatus(EnableStatus.ENABLED.code);
        loc = locationRepo.save(loc);

        AstAsset asset = buildAsset("AST-T-005", cat, bd("8000"), bd("0"), 12,
            DepreciationMethod.STRAIGHT_LINE);
        asset.setLocation(loc);
        assetRepo.save(asset);
        asset = commitAsset(asset);
        assertEquals("张三", asset.getCustodianName(), "初始保管人=张三");
        assertEquals(5001L, asset.getCostCenterId(), "提交不改 costCenterId(仍=5001)");

        // 转移单:保管人 张三→李四 / 成本中心 →9001,提交后同步回资产
        AstAssetMovement mv = new AstAssetMovement();
        mv.setNo("MV-T-005");
        mv.setAsset(asset);
        mv.setMovementType(MovementType.CUSTODIAN.code);
        mv.setFromCustodianName("张三");
        mv.setToCustodianName("李四");
        mv.setFromCostCenterId(5001L);
        mv.setToCostCenterId(9001L);
        mv.setStatus(MovementStatus.DRAFT.code);
        mv.setMovementDate(LocalDate.of(2026, 8, 31));
        mv = movementRepo.save(mv);

        String mvResult = movementLifecycle.exec(List.of(mv), null,
            new String[]{AstMovementLifecycleHandler.CODE_SUBMIT});
        assertTrue(mvResult.contains("成功 1"), "转移单提交应成功,实际=" + mvResult);
        AstAssetMovement submittedMv = movementRepo.findById(mv.getId()).orElseThrow();
        assertEquals(MovementStatus.SUBMITTED.code, submittedMv.getStatus(), "转移单状态=已提交");

        // 资产快照应同步
        AstAsset synced = assetRepo.findById(asset.getId()).orElseThrow();
        assertEquals("李四", synced.getCustodianName(), "资产保管人应同步为李四");
        assertEquals(9001L, synced.getCostCenterId(), "资产成本中心应同步为 9001");

        // 转移单取消不可(已提交非已取消可取消? 实际 applyCancel 允许非 CANCELLED → CANCELLED)
        String mvCancel = movementLifecycle.exec(List.of(submittedMv), null,
            new String[]{AstMovementLifecycleHandler.CODE_CANCEL});
        assertTrue(mvCancel.contains("成功 1"), "转移单取消应成功,实际=" + mvCancel);
        AstAssetMovement cancelledMv = movementRepo.findById(mv.getId()).orElseThrow();
        assertEquals(MovementStatus.CANCELLED.code, cancelledMv.getStatus(), "转移单状态=已取消");

        // 维修单1:完成 草稿→已完成
        AstAssetRepair r1 = new AstAssetRepair();
        r1.setNo("RP-T-005A");
        r1.setAsset(asset);
        r1.setRepairDate(LocalDate.of(2026, 8, 31));
        r1.setRepairCost(bd("300.00"));
        r1.setRepairerName("维修工王五");
        r1.setStatus(RepairStatus.DRAFT.code);
        r1.setDescription("屏幕更换");
        r1 = repairRepo.save(r1);
        String r1Result = repairLifecycle.exec(List.of(r1), null,
            new String[]{AstRepairLifecycleHandler.CODE_COMPLETE});
        assertTrue(r1Result.contains("成功 1"), "维修单完成应成功,实际=" + r1Result);
        AstAssetRepair done1 = repairRepo.findById(r1.getId()).orElseThrow();
        assertEquals(RepairStatus.COMPLETED.code, done1.getStatus(), "维修单状态=已完成");

        // 维修单2:取消 草稿→已取消
        AstAssetRepair r2 = new AstAssetRepair();
        r2.setNo("RP-T-005B");
        r2.setAsset(asset);
        r2.setRepairDate(LocalDate.of(2026, 8, 31));
        r2.setRepairCost(bd("150.00"));
        r2.setRepairerName("维修工赵六");
        r2.setStatus(RepairStatus.DRAFT.code);
        r2 = repairRepo.save(r2);
        String r2Result = repairLifecycle.exec(List.of(r2), null,
            new String[]{AstRepairLifecycleHandler.CODE_CANCEL});
        assertTrue(r2Result.contains("成功 1"), "维修单取消应成功,实际=" + r2Result);
        AstAssetRepair done2 = repairRepo.findById(r2.getId()).orElseThrow();
        assertEquals(RepairStatus.CANCELLED.code, done2.getStatus(), "维修单状态=已取消");
    }

    // =================== (6) 状态机守卫:非法迁移拒绝 + 状态字段禁止表单直改 ===================
    @Test
    void ast6_state_machine_guard() {
        seedGlAccounts();
        AstAssetCategory cat = seedCategory(DepreciationMethod.STRAIGHT_LINE, null, 12);
        AstAsset asset = buildAsset("AST-T-006", cat, bd("6000"), bd("0"), 12,
            DepreciationMethod.STRAIGHT_LINE);
        // (a) 草稿资产不可直接折旧(须先提交)
        String depResult = assetLifecycle.exec(List.of(asset), null,
            new String[]{AstAssetLifecycleHandler.CODE_DEPRECIATE});
        assertTrue(depResult.contains("失败 1"), "草稿资产折旧应被拒绝,实际=" + depResult);
        AstAsset stillDraft = assetRepo.findById(asset.getId()).orElseThrow();
        assertEquals(AssetStatus.DRAFT.code, stillDraft.getStatus(), "草稿折旧失败后状态不变");

        // (b) 提交后出售 → SOLD,再出售/报废应被拒绝
        AstAsset committed = commitAsset(asset);
        assetLifecycle.exec(List.of(committed), null,
            new String[]{AstAssetLifecycleHandler.CODE_SELL});
        AstAsset sold = assetRepo.findById(asset.getId()).orElseThrow();
        assertEquals(AssetStatus.SOLD.code, sold.getStatus());

        String reSell = assetLifecycle.exec(List.of(sold), null,
            new String[]{AstAssetLifecycleHandler.CODE_SELL});
        assertTrue(reSell.contains("失败 1"), "已出售再出售应被拒绝,实际=" + reSell);

        String scrapSold = assetLifecycle.exec(List.of(sold), null,
            new String[]{AstAssetLifecycleHandler.CODE_SCRAP});
        assertTrue(scrapSold.contains("失败 1"), "已出售报废应被拒绝,实际=" + scrapSold);

        // (c) 状态字段禁止表单直改:DataProxy beforeUpdate 在非草稿状态抛异常
        AstAsset.Proxy proxy = new AstAsset.Proxy();
        AstAsset draftAsset = new AstAsset();
        draftAsset.setStatus(AssetStatus.DRAFT.code);
        assertDoesNotThrow(() -> proxy.beforeUpdate(draftAsset), "草稿状态允许表单编辑");

        AstAsset availAsset = new AstAsset();
        availAsset.setStatus(AssetStatus.AVAILABLE.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(availAsset),
            "可用状态修改应抛异常:状态字段禁止表单直改,请通过行按钮变更");

        AstAsset soldAsset = new AstAsset();
        soldAsset.setStatus(AssetStatus.SOLD.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(soldAsset),
            "已出售状态修改应抛异常:状态字段禁止表单直改");
    }
}
