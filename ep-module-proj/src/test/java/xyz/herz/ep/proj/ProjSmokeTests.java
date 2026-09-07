package xyz.herz.ep.proj;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.AccountType;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.jpa.account.FinAccountRepository;
import xyz.herz.ep.fin.jpa.journal.FinJournalEntryRepository;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;
import xyz.herz.ep.proj.entity.cashflow.ProjCashFlow;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaim;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaimItem;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.entity.template.ProjProjectTemplate;
import xyz.herz.ep.proj.entity.template.ProjProjectTemplateTask;
import xyz.herz.ep.proj.entity.timesheet.ProjTimesheet;
import xyz.herz.ep.proj.enums.ProjDictEnums.ActivityCostType;
import xyz.herz.ep.proj.enums.ProjDictEnums.BillingStatus;
import xyz.herz.ep.proj.enums.ProjDictEnums.CashFlowType;
import xyz.herz.ep.proj.enums.ProjDictEnums.EnableStatus;
import xyz.herz.ep.proj.enums.ProjDictEnums.ExpenseClaimStatus;
import xyz.herz.ep.proj.enums.ProjDictEnums.ProjectStatus;
import xyz.herz.ep.proj.enums.ProjDictEnums.TaskStatus;
import xyz.herz.ep.proj.handler.expense.ProjExpenseClaimLifecycleHandler;
import xyz.herz.ep.proj.handler.master.ProjTemplateToggleHandler;
import xyz.herz.ep.proj.handler.project.ProjProjectLifecycleHandler;
import xyz.herz.ep.proj.handler.task.ProjTaskLifecycleHandler;
import xyz.herz.ep.proj.handler.timesheet.ProjTimesheetApproveHandler;
import xyz.herz.ep.proj.jpa.activitycost.ProjActivityCostRepository;
import xyz.herz.ep.proj.jpa.cashflow.ProjCashFlowRepository;
import xyz.herz.ep.proj.jpa.expense.ProjExpenseClaimRepository;
import xyz.herz.ep.proj.jpa.project.ProjProjectRepository;
import xyz.herz.ep.proj.jpa.task.ProjTaskRepository;
import xyz.herz.ep.proj.jpa.template.ProjProjectTemplateRepository;
import xyz.herz.ep.proj.jpa.timesheet.ProjTimesheetRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 项目管理模块冒烟测试(参考 ERPNext Projects DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-C 验收矩阵:
 * <ol>
 *   <li>Proj1 项目生命周期:立项/开工/完工/暂停/取消状态机 + 已完工不可取消</li>
 *   <li>Proj2 任务树进度:子任务完成 → 父任务 progress + 项目 percentComplete 回写</li>
 *   <li>Proj3 工时审批回写:hours/cost 计算 + 活动成本 + 任务 actualTime + 项目 totalCost</li>
 *   <li>Proj4 费用报销入账:提交/批准/入账 → 活动成本 + 现金流 + 项目 totalCost + GL(I-9)</li>
 *   <li>Proj5 现金流聚合:费用入账插入 OUTFLOW,按项目聚合校验</li>
 *   <li>Proj6 收入确认 GL:项目完工 → 借 AR / 贷 REV 凭证(I-10)</li>
 * </ol>
 *
 * <p>客户/员工/成本中心用快照,不 REF 跨模块,保持 proj 独立可测。
 * GL 凭证校验通过 FinJournalEntryRepository.findBySourceTypeAndSourceId。
 */
@SpringBootTest(classes = ProjTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class ProjSmokeTests {

    @Autowired ProjProjectRepository projectRepo;
    @Autowired ProjTaskRepository taskRepo;
    @Autowired ProjTimesheetRepository timesheetRepo;
    @Autowired ProjExpenseClaimRepository expenseRepo;
    @Autowired ProjCashFlowRepository cashFlowRepo;
    @Autowired ProjActivityCostRepository activityCostRepo;
    @Autowired ProjProjectTemplateRepository templateRepo;

    @Autowired FinAccountRepository finAccountRepo;
    @Autowired FinJournalEntryRepository journalRepo;
    @Autowired FinPostingFacade postingFacade;

    @Autowired ProjProjectLifecycleHandler projectLifecycle;
    @Autowired ProjTaskLifecycleHandler taskLifecycle;
    @Autowired ProjExpenseClaimLifecycleHandler expenseLifecycle;
    @Autowired ProjTimesheetApproveHandler timesheetApprove;
    @Autowired ProjTemplateToggleHandler templateToggle;

    /** 种 4 个总账科目:AR(资产)/REV(收入)/EXP(费用)/AP(负债),供 Handler 触发 GL。 */
    private void seedGlAccounts() {
        if (finAccountRepo.findByCode("AR").isEmpty()) {
            finAccountRepo.save(buildAccount("AR", "应收账款", AccountType.ASSET));
        }
        if (finAccountRepo.findByCode("REV").isEmpty()) {
            finAccountRepo.save(buildAccount("REV", "主营业务收入", AccountType.INCOME));
        }
        if (finAccountRepo.findByCode("EXP").isEmpty()) {
            finAccountRepo.save(buildAccount("EXP", "项目费用", AccountType.EXPENSE));
        }
        if (finAccountRepo.findByCode("AP").isEmpty()) {
            finAccountRepo.save(buildAccount("AP", "应付薪酬", AccountType.LIABILITY));
        }
    }

    private static FinAccount buildAccount(String code, String name, AccountType type) {
        FinAccount a = new FinAccount();
        a.setCode(code);
        a.setName(name);
        a.setAccountType(type.code);
        return a;
    }

    private ProjProject basicProject(String no, BigDecimal revenue) {
        ProjProject p = new ProjProject();
        p.setNo(no);
        p.setName("测试项目-" + no);
        p.setProjectType("交付类");
        p.setCustomerId(7001L);
        p.setCustomerName("测试客户A");
        p.setExpectedStart(LocalDate.of(2026, 9, 1));
        p.setExpectedEnd(LocalDate.of(2026, 12, 31));
        p.setTotalRevenue(revenue);
        p.setCostCenterId(8001L);
        p.setStatus(ProjectStatus.DRAFT.code);
        return projectRepo.save(p);
    }

    // =================== (1) 项目生命周期:立项/开工/完工/暂停/取消 ===================
    @Test
    void proj1_project_lifecycle() {
        ProjProject p = basicProject("PRJ-T-001", new BigDecimal("100000.00"));

        // 立项: DRAFT → APPROVED + grossMargin 回写
        String r1 = projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        assertTrue(r1.contains("成功 1"), "立项应成功,实际=" + r1);
        ProjProject afterApprove = projectRepo.findById(p.getId()).orElseThrow();
        assertEquals(ProjectStatus.APPROVED.code, afterApprove.getStatus());
        assertEquals(0, new BigDecimal("100000.00").compareTo(afterApprove.getTotalRevenue()));
        assertEquals(0, new BigDecimal("100000.00").compareTo(afterApprove.getGrossMargin()),
            "无成本时 grossMargin = totalRevenue");

        // 开工: APPROVED → IN_PROGRESS + actualStart
        String r2 = projectLifecycle.exec(List.of(afterApprove), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});
        assertTrue(r2.contains("成功 1"), "开工应成功,实际=" + r2);
        ProjProject afterStart = projectRepo.findById(p.getId()).orElseThrow();
        assertEquals(ProjectStatus.IN_PROGRESS.code, afterStart.getStatus());
        assertNotNull(afterStart.getActualStart(), "开工应记录 actualStart");

        // 暂停: IN_PROGRESS → ON_HOLD
        String r3 = projectLifecycle.exec(List.of(afterStart), null,
            new String[]{ProjProjectLifecycleHandler.CODE_PAUSE});
        assertTrue(r3.contains("成功 1"), "暂停应成功,实际=" + r3);
        assertEquals(ProjectStatus.ON_HOLD.code,
            projectRepo.findById(p.getId()).orElseThrow().getStatus());

        // 取消场景:新项目 → 立项 → 开工 → 取消
        ProjProject p2 = basicProject("PRJ-T-001B", new BigDecimal("50000.00"));
        projectLifecycle.exec(List.of(p2), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p2), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});
        String rCancel = projectLifecycle.exec(List.of(p2), null,
            new String[]{ProjProjectLifecycleHandler.CODE_CANCEL});
        assertTrue(rCancel.contains("成功 1"), "取消应成功,实际=" + rCancel);
        assertEquals(ProjectStatus.CANCELLED.code,
            projectRepo.findById(p2.getId()).orElseThrow().getStatus());

        // 已完工不可取消(OperationHandler 契约:捕获异常并返回失败串)
        seedGlAccounts();
        ProjProject p3 = basicProject("PRJ-T-001C", new BigDecimal("30000.00"));
        projectLifecycle.exec(List.of(p3), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p3), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});
        projectLifecycle.exec(List.of(p3), null,
            new String[]{ProjProjectLifecycleHandler.CODE_COMPLETE});
        String rReject = projectLifecycle.exec(List.of(p3), null,
            new String[]{ProjProjectLifecycleHandler.CODE_CANCEL});
        assertTrue(rReject.contains("失败 1") && rReject.contains("不可取消"),
            "已完工项目取消应被拒,实际=" + rReject);
    }

    // =================== (2) 任务树进度:子任务完成 → 父任务/项目进度回写 ===================
    @Test
    void proj2_task_tree_progress() {
        ProjProject p = basicProject("PRJ-T-002", new BigDecimal("200000.00"));
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});
        assertEquals(ProjectStatus.IN_PROGRESS.code,
            projectRepo.findById(p.getId()).orElseThrow().getStatus());

        // 父任务 + 2 个子任务
        ProjTask parent = new ProjTask();
        parent.setSubject("需求拆解(父)");
        parent.setProject(p);
        parent.setStatus(TaskStatus.NOT_STARTED.code);
        parent = taskRepo.save(parent);

        ProjTask c1 = new ProjTask();
        c1.setSubject("子任务 1");
        c1.setProject(p);
        c1.setParentTask(parent);
        c1.setStatus(TaskStatus.NOT_STARTED.code);
        c1 = taskRepo.save(c1);

        ProjTask c2 = new ProjTask();
        c2.setSubject("子任务 2");
        c2.setProject(p);
        c2.setParentTask(parent);
        c2.setStatus(TaskStatus.NOT_STARTED.code);
        c2 = taskRepo.save(c2);

        // 子1 开工 → 完成
        taskLifecycle.exec(List.of(c1), null,
            new String[]{ProjTaskLifecycleHandler.CODE_START});
        String r1 = taskLifecycle.exec(List.of(c1), null,
            new String[]{ProjTaskLifecycleHandler.CODE_COMPLETE});
        assertTrue(r1.contains("成功 1"), "子1完成应成功,实际=" + r1);
        assertEquals(TaskStatus.COMPLETED.code,
            taskRepo.findById(c1.getId()).orElseThrow().getStatus());

        // 父任务 progress = avg(100, 0) = 50
        ProjTask parentAfter1 = taskRepo.findById(parent.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(parentAfter1.getProgress()),
            "父任务进度应为 50(1/2 子完成),实际=" + parentAfter1.getProgress());

        // 项目 percentComplete = 1/3 完成(父+2子,只子1完成)
        ProjProject projAfter1 = projectRepo.findById(p.getId()).orElseThrow();
        assertNotNull(projAfter1.getPercentComplete());

        // 子2 完成 → 父任务全完成,父 progress=100 且状态 COMPLETED
        taskLifecycle.exec(List.of(c2), null,
            new String[]{ProjTaskLifecycleHandler.CODE_START});
        taskLifecycle.exec(List.of(c2), null,
            new String[]{ProjTaskLifecycleHandler.CODE_COMPLETE});
        ProjTask parentAfter2 = taskRepo.findById(parent.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(parentAfter2.getProgress()),
            "全部子完成后父进度应 100");
        assertEquals(TaskStatus.COMPLETED.code, parentAfter2.getStatus());
    }

    // =================== (3) 工时审批回写:hours/cost + 活动成本 + 任务工时 + 项目成本 + GL ===================
    @Test
    void proj3_timesheet_cost_rollup() {
        seedGlAccounts();
        ProjProject p = basicProject("PRJ-T-003", new BigDecimal("150000.00"));
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});

        ProjTask t = new ProjTask();
        t.setSubject("开发任务");
        t.setProject(p);
        t.setStatus(TaskStatus.NOT_STARTED.code);
        t = taskRepo.save(t);
        taskLifecycle.exec(List.of(t), null,
            new String[]{ProjTaskLifecycleHandler.CODE_START});

        // 工时表:2 小时(7200 秒),费率 200/小时 → cost = 400
        ProjTimesheet ts = new ProjTimesheet();
        ts.setEmployeeId(9001L);
        ts.setEmployeeName("李工时");
        ts.setProject(p);
        ts.setTask(t);
        LocalDateTime from = LocalDateTime.of(2026, 9, 15, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 15, 11, 0);
        ts.setFromTime(from);
        ts.setToTime(to);
        ts.setBillingRate(new BigDecimal("200.00"));
        ts.setBillingStatus(BillingStatus.UNBILLED.code);
        ts = timesheetRepo.save(ts);

        String r = timesheetApprove.exec(List.of(ts), null,
            new String[]{ProjTimesheetApproveHandler.CODE_APPROVE});
        assertTrue(r.contains("成功 1"), "工时审批应成功,实际=" + r);

        ProjTimesheet after = timesheetRepo.findById(ts.getId()).orElseThrow();
        assertEquals(BillingStatus.BILLED.code, after.getBillingStatus());
        assertEquals(0, new BigDecimal("2.0000").compareTo(after.getHours()),
            "hours 应为 2,实际=" + after.getHours());
        assertEquals(0, new BigDecimal("400.000000").compareTo(after.getCost()),
            "cost 应为 400,实际=" + after.getCost());

        // 活动成本行(LABOR)
        List<ProjActivityCost> costs = activityCostRepo.findByProjectId(p.getId());
        assertFalse(costs.isEmpty(), "应产生活动成本行");
        ProjActivityCost ac = costs.get(0);
        assertEquals(ActivityCostType.LABOR.code, ac.getCostType());
        assertEquals(0, new BigDecimal("400.000000").compareTo(ac.getAmount()));

        // 任务 actualTime 回写
        ProjTask tAfter = taskRepo.findById(t.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("2.0000").compareTo(tAfter.getActualTime()),
            "任务 actualTime 应累加 2,实际=" + tAfter.getActualTime());

        // 项目 totalCost 回写
        ProjProject pAfter = projectRepo.findById(p.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("400.000000").compareTo(pAfter.getTotalCost()),
            "项目 totalCost 应累加 400,实际=" + pAfter.getTotalCost());

        // GL 凭证(I-11):sourceType=ProjectExpense
        List<FinJournalEntry> jes = journalRepo.findBySourceTypeAndSourceId("ProjectExpense", ts.getId());
        assertFalse(jes.isEmpty(), "工时审批应产生 GL 凭证");
    }

    // =================== (4) 费用报销入账:活动成本 + 现金流 + 项目成本 + GL ===================
    @Test
    void proj4_expense_claim_post_gl() {
        seedGlAccounts();
        ProjProject p = basicProject("PRJ-T-004", new BigDecimal("120000.00"));
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});

        ProjExpenseClaim claim = new ProjExpenseClaim();
        claim.setNo("EX-T-004");
        claim.setEmployeeId(9101L);
        claim.setEmployeeName("王报销");
        claim.setProject(p);
        claim.setStatus(ExpenseClaimStatus.DRAFT.code);
        ProjExpenseClaimItem i1 = new ProjExpenseClaimItem();
        i1.setClaim(claim);
        i1.setDescription("差旅费");
        i1.setAmount(new BigDecimal("3000.00"));
        ProjExpenseClaimItem i2 = new ProjExpenseClaimItem();
        i2.setClaim(claim);
        i2.setDescription("招待费");
        i2.setAmount(new BigDecimal("2000.00"));
        claim.setItems(new ArrayList<>(List.of(i1, i2)));
        claim = expenseRepo.save(claim);

        // 提交:totalAmount = 5000
        String r1 = expenseLifecycle.exec(List.of(claim), null,
            new String[]{ProjExpenseClaimLifecycleHandler.CODE_SUBMIT});
        assertTrue(r1.contains("成功 1"), "提交应成功,实际=" + r1);
        assertEquals(0, new BigDecimal("5000.00").compareTo(
            expenseRepo.findById(claim.getId()).orElseThrow().getTotalAmount()));

        // 批准:sanctionedAmount = 5000
        String r2 = expenseLifecycle.exec(List.of(claim), null,
            new String[]{ProjExpenseClaimLifecycleHandler.CODE_APPROVE});
        assertTrue(r2.contains("成功 1"), "批准应成功,实际=" + r2);

        // 入账:POSTED + 活动成本 + 现金流 + 项目成本 + GL
        String r3 = expenseLifecycle.exec(List.of(claim), null,
            new String[]{ProjExpenseClaimLifecycleHandler.CODE_POST});
        assertTrue(r3.contains("成功 1"), "入账应成功,实际=" + r3);
        assertEquals(ExpenseClaimStatus.POSTED.code,
            expenseRepo.findById(claim.getId()).orElseThrow().getStatus());

        // 活动成本(EXPENSE)
        List<ProjActivityCost> costs = activityCostRepo.findByProjectId(p.getId());
        boolean hasExpense = costs.stream().anyMatch(c ->
            c.getCostType() == ActivityCostType.EXPENSE.code
            && new BigDecimal("5000.00").compareTo(c.getAmount()) == 0);
        assertTrue(hasExpense, "应产生 EXPENSE 活动成本 5000");

        // 现金流(OUTFLOW 5000)
        List<ProjCashFlow> cfs = cashFlowRepo.findByProjectId(p.getId());
        assertFalse(cfs.isEmpty(), "应产生现金流");
        ProjCashFlow cf = cfs.get(0);
        assertEquals(CashFlowType.OUTFLOW.code, cf.getFlowType());
        assertEquals(0, new BigDecimal("5000.00").compareTo(cf.getAmount()));

        // 项目 totalCost 累加 5000
        ProjProject pAfter = projectRepo.findById(p.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5000.00").compareTo(pAfter.getTotalCost()),
            "项目 totalCost 应累加 5000,实际=" + pAfter.getTotalCost());

        // GL 凭证(I-9):sourceType=ProjectExpense
        List<FinJournalEntry> jes = journalRepo.findBySourceTypeAndSourceId("ProjectExpense", claim.getId());
        assertFalse(jes.isEmpty(), "费用入账应产生 GL 凭证");
    }

    // =================== (5) 现金流聚合:按项目聚合流入/流出 ===================
    @Test
    void proj5_cash_flow_aggregation() {
        ProjProject p = basicProject("PRJ-T-005", new BigDecimal("80000.00"));

        // 手动插 2 笔 OUTFLOW + 1 笔 INFLOW,模拟多源资金流
        cashFlowRepo.save(buildCf(p, CashFlowType.OUTFLOW.code, new BigDecimal("1000.00"), "ExpenseClaim", 1001L, "EX-A"));
        cashFlowRepo.save(buildCf(p, CashFlowType.OUTFLOW.code, new BigDecimal("2000.00"), "ExpenseClaim", 1002L, "EX-B"));
        cashFlowRepo.save(buildCf(p, CashFlowType.INFLOW.code, new BigDecimal("5000.00"), "SalesReceipt", 1003L, "SR-A"));

        List<ProjCashFlow> cfs = cashFlowRepo.findByProjectId(p.getId());
        assertEquals(3, cfs.size(), "应 3 笔现金流");

        BigDecimal outflow = cfs.stream()
            .filter(c -> c.getFlowType() == CashFlowType.OUTFLOW.code)
            .map(ProjCashFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inflow = cfs.stream()
            .filter(c -> c.getFlowType() == CashFlowType.INFLOW.code)
            .map(ProjCashFlow::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("3000.00").compareTo(outflow), "流出合计 3000");
        assertEquals(0, new BigDecimal("5000.00").compareTo(inflow), "流入合计 5000");
        assertEquals(0, new BigDecimal("2000.00").compareTo(inflow.subtract(outflow)),
            "净流 2000");
    }

    private static ProjCashFlow buildCf(ProjProject p, int flowType, BigDecimal amount,
                                        String sourceType, Long sourceId, String sourceNo) {
        ProjCashFlow cf = new ProjCashFlow();
        cf.setProject(p);
        cf.setFlowType(flowType);
        cf.setAmount(amount);
        cf.setPostingDate(LocalDate.now());
        cf.setSourceType(sourceType);
        cf.setSourceId(sourceId);
        cf.setSourceNo(sourceNo);
        return cf;
    }

    // =================== (6) 收入确认 GL:项目完工 → 借 AR / 贷 REV ===================
    @Test
    void proj6_revenue_recognition() {
        seedGlAccounts();
        ProjProject p = basicProject("PRJ-T-006", new BigDecimal("88888.00"));
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_APPROVE});
        projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_START});

        // 完工:触发收入确认 GL(I-10)
        String r = projectLifecycle.exec(List.of(p), null,
            new String[]{ProjProjectLifecycleHandler.CODE_COMPLETE});
        assertTrue(r.contains("成功 1"), "完工应成功,实际=" + r);
        ProjProject after = projectRepo.findById(p.getId()).orElseThrow();
        assertEquals(ProjectStatus.COMPLETED.code, after.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(after.getPercentComplete()));
        assertNotNull(after.getActualEnd(), "完工应记录 actualEnd");

        // GL 凭证(I-10):sourceType=ProjectRevenue
        List<FinJournalEntry> jes = journalRepo.findBySourceTypeAndSourceId("ProjectRevenue", p.getId());
        assertFalse(jes.isEmpty(), "项目完工应产生收入确认 GL 凭证");
        FinJournalEntry je = jes.get(0);
        assertEquals(0, new BigDecimal("88888.00").compareTo(je.getTotalDebit()),
            "借方应 = totalRevenue 88888,实际=" + je.getTotalDebit());
        assertEquals(0, new BigDecimal("88888.00").compareTo(je.getTotalCredit()),
            "贷方应 = totalRevenue 88888,实际=" + je.getTotalCredit());

        // 模板启停切换(顺带验证 ProjTemplateToggleHandler)
        ProjProjectTemplate tpl = new ProjProjectTemplate();
        tpl.setCode("TPL-" + System.nanoTime());
        tpl.setName("测试模板");
        tpl.setStatus(EnableStatus.ENABLED.code);
        ProjProjectTemplateTask tt = new ProjProjectTemplateTask();
        tt.setTemplate(tpl);
        tt.setSubject("模板任务A");
        tt.setExpectedTime(new BigDecimal("8.00"));
        tpl.setTasks(new ArrayList<>(List.of(tt)));
        templateRepo.save(tpl);

        String rDisable = templateToggle.exec(List.of(tpl), null,
            new String[]{ProjTemplateToggleHandler.CODE_DISABLE});
        assertTrue(rDisable.contains("成功 1"), "模板停用应成功,实际=" + rDisable);
        assertEquals(EnableStatus.DISABLED.code,
            templateRepo.findById(tpl.getId()).orElseThrow().getStatus());
    }
}
