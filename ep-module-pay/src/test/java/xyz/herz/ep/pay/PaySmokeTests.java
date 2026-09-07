package xyz.herz.ep.pay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.AccountType;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.jpa.account.FinAccountRepository;
import xyz.herz.ep.fin.jpa.journal.FinJournalEntryRepository;
import xyz.herz.ep.hr.entity.department.HrDepartment;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.hr.enums.HrDictEnums.EmployeeStatus;
import xyz.herz.ep.hr.jpa.department.HrDepartmentRepository;
import xyz.herz.ep.hr.jpa.employee.HrEmployeeRepository;
import xyz.herz.ep.pay.entity.component.PaySalaryComponent;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;
import xyz.herz.ep.pay.entity.slip.PaySalarySlipItem;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructure;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructureItem;
import xyz.herz.ep.pay.enums.PayDictEnums.ComponentType;
import xyz.herz.ep.pay.enums.PayDictEnums.SlipStatus;
import xyz.herz.ep.pay.handler.slip.PaySlipLifecycleHandler;
import xyz.herz.ep.pay.jpa.component.PaySalaryComponentRepository;
import xyz.herz.ep.pay.jpa.slip.PaySalarySlipRepository;
import xyz.herz.ep.pay.jpa.structure.PaySalaryStructureRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 薪酬管理模块冒烟测试(参考 ERPNext Payroll DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-G 验收矩阵:
 * <ol>
 *   <li>Pay1 组件/结构主数据:收入+扣款组件 + 结构明细(基本工资/社保/公积金)</li>
 *   <li>Pay2 工资单提交自动填充:从结构快照生成明细 + gross/deductions/net 派生</li>
 *   <li>Pay3 工资单 GL 过账:借 SALARY_EXP 总额 = 贷 SALARY_PAY 实发 + 贷 SALARY_WITHHOLD 代扣</li>
 *   <li>Pay4 工资单取消:已过账取消 + GL 反向冲销 + 已取消不可再取消</li>
 *   <li>Pay5 多员工批量:2 员工独立工资单 + 按薪酬月份查询</li>
 *   <li>Pay6 状态机守卫:草稿不可过账/已过账不可再过账/status 字段禁止表单直改</li>
 * </ol>
 *
 * <p>科目码快照(SALARY_EXP/SALARY_PAY/SALARY_WITHHOLD),测试预种 3 个 FinAccount;
 * 员工 REF hr 模块;过账通过 FinPostingFacade,凭证来源 JournalSourceType.SALARY。
 */
@SpringBootTest(classes = PayTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class PaySmokeTests {

    @Autowired PaySalaryComponentRepository componentRepo;
    @Autowired PaySalaryStructureRepository structureRepo;
    @Autowired PaySalarySlipRepository slipRepo;
    @Autowired HrEmployeeRepository empRepo;
    @Autowired HrDepartmentRepository deptRepo;
    @Autowired FinAccountRepository accountRepo;
    @Autowired FinJournalEntryRepository journalRepo;
    @Autowired PaySlipLifecycleHandler slipLifecycle;

    private static final String ACC_EXP = PaySlipLifecycleHandler.ACC_SALARY_EXPENSE;
    private static final String ACC_PAY = PaySlipLifecycleHandler.ACC_SALARY_PAYABLE;
    private static final String ACC_WH = PaySlipLifecycleHandler.ACC_SALARY_WITHHOLDING;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private void seedGlAccounts() {
        accountRepo.save(buildAccount(ACC_EXP, "工资费用", AccountType.EXPENSE));
        accountRepo.save(buildAccount(ACC_PAY, "应付职工薪酬", AccountType.LIABILITY));
        accountRepo.save(buildAccount(ACC_WH, "代扣款项", AccountType.LIABILITY));
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

    private PaySalaryComponent buildComponent(String code, String name, ComponentType type) {
        PaySalaryComponent c = new PaySalaryComponent();
        c.setCode(code); c.setName(name);
        c.setComponentType(type.code);
        c.setIsTaxable(true);
        c.setStatus(EnableStatus.ENABLED.code);
        return componentRepo.save(c);
    }

    /** 标准结构:基本工资 10000(收入) / 社保 -2000 / 公积金 -1200(扣款)。 */
    private PaySalaryStructure seedStdStructure() {
        PaySalaryComponent basic = buildComponent("BASIC", "基本工资", ComponentType.EARNING);
        PaySalaryComponent ss = buildComponent("SS", "社保个人", ComponentType.DEDUCTION);
        PaySalaryComponent hf = buildComponent("HF", "公积金个人", ComponentType.DEDUCTION);

        PaySalaryStructure st = new PaySalaryStructure();
        st.setCode("STD"); st.setName("标准工资结构");
        st.setIsActive(true); st.setStatus(EnableStatus.ENABLED.code);

        PaySalaryStructureItem i1 = new PaySalaryStructureItem();
        i1.setStructure(st); i1.setComponent(basic); i1.setAmount(bd("10000.00"));
        PaySalaryStructureItem i2 = new PaySalaryStructureItem();
        i2.setStructure(st); i2.setComponent(ss); i2.setAmount(bd("2000.00"));
        PaySalaryStructureItem i3 = new PaySalaryStructureItem();
        i3.setStructure(st); i3.setComponent(hf); i3.setAmount(bd("1200.00"));
        st.getItems().addAll(List.of(i1, i2, i3));
        return structureRepo.save(st);
    }

    private HrEmployee seedActiveEmployee(String empNo) {
        HrDepartment dept = new HrDepartment();
        dept.setCode("D-" + empNo); dept.setName("部门-" + empNo);
        dept.setStatus(EnableStatus.ENABLED.code);
        dept = deptRepo.save(dept);

        HrEmployee e = new HrEmployee();
        e.setEmpNo(empNo); e.setName("员工-" + empNo);
        e.setDepartment(dept);
        e.setStatus(EmployeeStatus.ACTIVE.code);
        e.setHireDate(java.time.LocalDate.of(2026, 1, 15));
        return empRepo.save(e);
    }

    private PaySalarySlip buildSlip(String slipNo, HrEmployee emp, PaySalaryStructure st, String month) {
        PaySalarySlip s = new PaySalarySlip();
        s.setSlipNo(slipNo); s.setEmployee(emp); s.setStructure(st);
        s.setPostingMonth(month);
        s.setStatus(SlipStatus.DRAFT.code);
        return slipRepo.save(s);
    }

    private PaySalarySlip submitSlip(PaySalarySlip s) {
        String r = slipLifecycle.exec(List.of(s), null,
            new String[]{PaySlipLifecycleHandler.CODE_SUBMIT});
        assertTrue(r.contains("成功 1"), "提交应成功,实际=" + r);
        return slipRepo.findById(s.getId()).orElseThrow();
    }

    // =================== (1) 组件/结构主数据 ===================
    @Test
    void pay1_component_and_structure_master() {
        PaySalaryStructure st = seedStdStructure();

        List<PaySalaryStructureItem> items = st.getItems();
        assertEquals(3, items.size(), "结构明细应为 3 行");
        assertEquals(ComponentType.EARNING.code,
            items.get(0).getComponent().getComponentType(), "第 1 行应为收入项");
        assertEquals(ComponentType.DEDUCTION.code,
            items.get(1).getComponent().getComponentType(), "第 2 行应为扣款项");
        assertEquals(0, items.get(0).getAmount().compareTo(bd("10000.00")), "基本工资=10000");

        List<PaySalaryComponent> comps = componentRepo.findByStatus(EnableStatus.ENABLED.code);
        assertEquals(3, comps.size(), "启用组件应为 3 个");
        assertTrue(structureRepo.findByCode("STD").isPresent(), "STD 结构应存在");
    }

    // =================== (2) 工资单提交自动填充 ===================
    @Test
    void pay2_slip_submit_autofill() {
        seedGlAccounts();
        PaySalaryStructure st = seedStdStructure();
        HrEmployee emp = seedActiveEmployee("E-P-001");
        PaySalarySlip slip = buildSlip("SLIP-T-001", emp, st, "2026-09");

        // 提交:草稿 → 已提交,自动从结构生成明细
        PaySalarySlip submitted = submitSlip(slip);
        assertEquals(SlipStatus.SUBMITTED.code, submitted.getStatus());

        List<PaySalarySlipItem> items = submitted.getItems();
        assertEquals(3, items.size(), "明细快照应为 3 行");
        assertEquals("BASIC", items.get(0).getComponentCode(), "明细应快照组件编码");
        assertEquals("基本工资", items.get(0).getComponentName(), "明细应快照组件名称");
        assertEquals(0, submitted.getGrossPay().compareTo(bd("10000.00")), "应发=10000");
        assertEquals(0, submitted.getDeductions().compareTo(bd("3200.00")), "扣款=2000+1200=3200");
        assertEquals(0, submitted.getNetPay().compareTo(bd("6800.00")), "实发=10000-3200=6800");
    }

    // =================== (3) 工资单 GL 过账 ===================
    @Test
    void pay3_slip_post_gl() {
        seedGlAccounts();
        PaySalaryStructure st = seedStdStructure();
        HrEmployee emp = seedActiveEmployee("E-P-002");
        PaySalarySlip slip = buildSlip("SLIP-T-002", emp, st, "2026-09");
        PaySalarySlip submitted = submitSlip(slip);

        // 过账:已提交 → 已过账,GL 借 10000 = 贷 6800 + 贷 3200
        String postR = slipLifecycle.exec(List.of(submitted), null,
            new String[]{PaySlipLifecycleHandler.CODE_POST});
        assertTrue(postR.contains("成功 1"), "过账应成功,实际=" + postR);
        PaySalarySlip posted = slipRepo.findById(slip.getId()).orElseThrow();
        assertEquals(SlipStatus.POSTED.code, posted.getStatus(), "过账后状态=已过账");
        assertNotNull(posted.getJournalEntryId(), "凭证 ID 应回写");

        List<FinJournalEntry> journals = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.SALARY.code, posted.getId());
        assertEquals(1, journals.size(), "应生成 1 张工资凭证");
        FinJournalEntry je = journals.get(0);
        assertEquals(0, je.getTotalDebit().compareTo(bd("10000.00")), "借方合计=gross 10000");
        assertEquals(0, je.getTotalCredit().compareTo(bd("10000.00")), "贷方合计=6800+3200=10000");
        assertTrue(je.getItems().stream().anyMatch(i ->
            ACC_EXP.equals(i.getAccount().getCode()) && i.getDebit().compareTo(bd("10000.00")) == 0),
            "凭证应含 SALARY_EXP 借方 10000(工资费用)");
        assertTrue(je.getItems().stream().anyMatch(i ->
            ACC_PAY.equals(i.getAccount().getCode()) && i.getCredit().compareTo(bd("6800.00")) == 0),
            "凭证应含 SALARY_PAY 贷方 6800(实发)");
        assertTrue(je.getItems().stream().anyMatch(i ->
            ACC_WH.equals(i.getAccount().getCode()) && i.getCredit().compareTo(bd("3200.00")) == 0),
            "凭证应含 SALARY_WITHHOLD 贷方 3200(代扣)");
    }

    // =================== (4) 工资单取消 + GL 反向冲销 ===================
    @Test
    void pay4_slip_cancel_reversal() {
        seedGlAccounts();
        PaySalaryStructure st = seedStdStructure();
        HrEmployee emp = seedActiveEmployee("E-P-003");
        PaySalarySlip slip = buildSlip("SLIP-T-003", emp, st, "2026-09");
        PaySalarySlip submitted = submitSlip(slip);
        slipLifecycle.exec(List.of(submitted), null,
            new String[]{PaySlipLifecycleHandler.CODE_POST});

        // 取消已过账工资单 → GL 反向冲销
        PaySalarySlip posted = slipRepo.findById(slip.getId()).orElseThrow();
        String cancelR = slipLifecycle.exec(List.of(posted), null,
            new String[]{PaySlipLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("成功 1"), "取消应成功,实际=" + cancelR);
        PaySalarySlip cancelled = slipRepo.findById(slip.getId()).orElseThrow();
        assertEquals(SlipStatus.CANCELLED.code, cancelled.getStatus(), "取消后状态=已取消");
        assertNull(cancelled.getJournalEntryId(), "取消后凭证 ID 应清空");

        // 已取消不可再取消
        String reCancel = slipLifecycle.exec(List.of(cancelled), null,
            new String[]{PaySlipLifecycleHandler.CODE_CANCEL});
        assertTrue(reCancel.contains("失败 1"), "已取消不可再取消,实际=" + reCancel);
    }

    // =================== (5) 多员工批量 + 按薪酬月份查询 ===================
    @Test
    void pay5_multi_employee_and_month_query() {
        seedGlAccounts();
        PaySalaryStructure st = seedStdStructure();
        HrEmployee e1 = seedActiveEmployee("E-P-004");
        HrEmployee e2 = seedActiveEmployee("E-P-005");

        PaySalarySlip s1 = submitSlip(buildSlip("SLIP-T-004", e1, st, "2026-09"));
        PaySalarySlip s2 = submitSlip(buildSlip("SLIP-T-005", e2, st, "2026-09"));

        assertEquals(0, s1.getNetPay().compareTo(bd("6800.00")), "员工1 实发=6800");
        assertEquals(0, s2.getNetPay().compareTo(bd("6800.00")), "员工2 实发=6800");
        assertEquals(2, slipRepo.findByPostingMonth("2026-09").size(), "2026-09 期应 2 张工资单");
        assertEquals(1, slipRepo.findByEmployeeId(e1.getId()).size(), "员工1 名下 1 张工资单");
    }

    // =================== (6) 状态机守卫 ===================
    @Test
    void pay6_state_machine_guard() {
        seedGlAccounts();
        PaySalaryStructure st = seedStdStructure();
        HrEmployee emp = seedActiveEmployee("E-P-006");

        // (a) 草稿直接过账应被拒(须先提交)
        PaySalarySlip draft = buildSlip("SLIP-T-006", emp, st, "2026-09");
        String postDraft = slipLifecycle.exec(List.of(draft), null,
            new String[]{PaySlipLifecycleHandler.CODE_POST});
        assertTrue(postDraft.contains("失败 1"), "草稿过账应被拒,实际=" + postDraft);
        assertEquals(SlipStatus.DRAFT.code,
            slipRepo.findById(draft.getId()).orElseThrow().getStatus(), "草稿状态不变");

        // (b) 提交→过账后,再过账应被拒
        PaySalarySlip slip = buildSlip("SLIP-T-007", emp, st, "2026-10");
        PaySalarySlip submitted = submitSlip(slip);
        slipLifecycle.exec(List.of(submitted), null,
            new String[]{PaySlipLifecycleHandler.CODE_POST});
        PaySalarySlip posted = slipRepo.findById(slip.getId()).orElseThrow();
        assertEquals(SlipStatus.POSTED.code, posted.getStatus());
        String rePost = slipLifecycle.exec(List.of(posted), null,
            new String[]{PaySlipLifecycleHandler.CODE_POST});
        assertTrue(rePost.contains("失败 1"), "已过账再过账应被拒,实际=" + rePost);

        // (c) DataProxy beforeUpdate:status 字段禁止表单直改(非草稿抛异常)
        PaySalarySlip.Proxy proxy = new PaySalarySlip.Proxy();
        PaySalarySlip draftE = new PaySalarySlip();
        draftE.setStatus(SlipStatus.DRAFT.code);
        assertDoesNotThrow(() -> proxy.beforeUpdate(draftE), "草稿状态允许表单编辑");

        PaySalarySlip submittedE = new PaySalarySlip();
        submittedE.setStatus(SlipStatus.SUBMITTED.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(submittedE),
            "已提交状态修改应抛异常:status 禁止表单直改");

        PaySalarySlip postedE = new PaySalarySlip();
        postedE.setStatus(SlipStatus.POSTED.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(postedE),
            "已过账状态修改应抛异常:status 禁止表单直改");
    }
}
