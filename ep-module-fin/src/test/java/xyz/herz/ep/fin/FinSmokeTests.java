package xyz.herz.ep.fin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.budget.FinBudget;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.entity.payment.FinPaymentEntry;
import xyz.herz.ep.fin.enums.FinDictEnums.AccountType;
import xyz.herz.ep.fin.enums.FinDictEnums.BudgetStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.InvoiceStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.PaymentType;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.handler.account.FinAccountToggleHandler;
import xyz.herz.ep.fin.handler.budget.FinBudgetApproveHandler;
import xyz.herz.ep.fin.handler.invoice.FinPurchaseInvoiceSubmitHandler;
import xyz.herz.ep.fin.handler.invoice.FinSalesInvoiceSubmitHandler;
import xyz.herz.ep.fin.handler.payment.FinPaymentSubmitHandler;
import xyz.herz.ep.fin.jpa.account.FinAccountRepository;
import xyz.herz.ep.fin.jpa.budget.FinBudgetRepository;
import xyz.herz.ep.fin.jpa.costcenter.FinCostCenterRepository;
import xyz.herz.ep.fin.jpa.invoice.FinPurchaseInvoiceRepository;
import xyz.herz.ep.fin.jpa.invoice.FinSalesInvoiceRepository;
import xyz.herz.ep.fin.jpa.journal.FinJournalEntryRepository;
import xyz.herz.ep.fin.jpa.payment.FinPaymentEntryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 财务模块冒烟测试(参考 ERPNext Accounting DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-A 验收矩阵:
 * <ol>
 *   <li>Fin1 科目树 + 启停切换(行按钮)</li>
 *   <li>Fin2 凭证平账校验 + 反向冲销(cancel)</li>
 *   <li>Fin3 销售发票提交 → GL 借应收/贷收入</li>
 *   <li>Fin4 采购发票提交 → GL 借费用/贷应付</li>
 *   <li>Fin5 收付款单提交 → GL(收款:借银行/贷应收)+ 回写 generatedJournalId</li>
 *   <li>Fin6 预算批准 + 成本中心 + 预算使用率</li>
 * </ol>
 *
 * <p>客户/供应商用 partyId+partyName 快照,不 REF 跨模块,保持 fin 独立可测。
 */
@SpringBootTest(classes = FinTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class FinSmokeTests {

    @Autowired FinAccountRepository accountRepo;
    @Autowired FinCostCenterRepository costCenterRepo;
    @Autowired FinBudgetRepository budgetRepo;
    @Autowired FinJournalEntryRepository journalRepo;
    @Autowired FinSalesInvoiceRepository salesInvoiceRepo;
    @Autowired FinPurchaseInvoiceRepository purchaseInvoiceRepo;
    @Autowired FinPaymentEntryRepository paymentRepo;

    @Autowired FinPostingFacade postingFacade;
    @Autowired FinAccountToggleHandler accountToggleHandler;
    @Autowired FinBudgetApproveHandler budgetApproveHandler;
    @Autowired FinSalesInvoiceSubmitHandler salesSubmitHandler;
    @Autowired FinPurchaseInvoiceSubmitHandler purchaseSubmitHandler;
    @Autowired FinPaymentSubmitHandler paymentSubmitHandler;

    /** 种 5 个总账科目: AR(资产)/REV(收入)/EXP(费用)/AP(负债)/CASH(资产),供 Handler 触发 GL 使用。 */
    private void seedGlAccounts() {
        accountRepo.save(buildAccount("AR", "应收账款", AccountType.ASSET));
        accountRepo.save(buildAccount("REV", "主营业务收入", AccountType.INCOME));
        accountRepo.save(buildAccount("EXP", "采购费用", AccountType.EXPENSE));
        accountRepo.save(buildAccount("AP", "应付账款", AccountType.LIABILITY));
        accountRepo.save(buildAccount("CASH", "银行存款", AccountType.ASSET));
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

    // =================== (1) 科目树 + 启停切换 ===================
    @Test
    void fin1_account_tree_and_toggle() {
        FinAccount parent = buildAccount("ASSET-GRP", "资产类(组)", AccountType.ASSET);
        parent.setIsGroup(true);
        parent = accountRepo.save(parent);

        FinAccount child = buildAccount("AR-001", "应收账款-A001", AccountType.ASSET);
        child.setParent(parent);
        child = accountRepo.save(child);
        assertEquals(EnableStatus.ENABLED.code, child.getStatus());

        // 树关联: child.parent → parent
        FinAccount loaded = accountRepo.findById(child.getId()).orElseThrow();
        assertNotNull(loaded.getParent(), "子科目应能回查父科目");
        assertEquals(parent.getId(), loaded.getParent().getId());

        // 行按钮停用 → DISABLED
        String r1 = accountToggleHandler.exec(List.of(child), null,
            new String[]{FinAccountToggleHandler.DISABLE});
        assertTrue(r1.contains("成功 1"), "停用应成功,实际=" + r1);
        FinAccount afterDisable = accountRepo.findById(child.getId()).orElseThrow();
        assertEquals(EnableStatus.DISABLED.code, afterDisable.getStatus());

        // 行按钮重新启用 → ENABLED
        String r2 = accountToggleHandler.exec(List.of(afterDisable), null,
            new String[]{FinAccountToggleHandler.ENABLE});
        assertTrue(r2.contains("成功 1"), "启用应成功,实际=" + r2);
        FinAccount afterEnable = accountRepo.findById(child.getId()).orElseThrow();
        assertEquals(EnableStatus.ENABLED.code, afterEnable.getStatus());
    }

    // =================== (2) 凭证平账校验 + 反向冲销 ===================
    @Test
    void fin2_journal_balance_and_cancel() {
        seedGlAccounts();
        BigDecimal amt = new BigDecimal("1000.00");

        // (a) 平衡过账成功 → SUBMITTED 凭证
        Long jeId = postingFacade.post(new FinPostingFacade.PostingRequest(
            JournalSourceType.MANUAL_JOURNAL.code, 9001L, "T-MAN-001",
            LocalDate.of(2026, 8, 31),
            List.of(
                new FinPostingFacade.PostingLine("CASH", amt, null, null, null, null, null, "借银行"),
                new FinPostingFacade.PostingLine("REV", null, amt, null, null, null, null, "贷收入")
            ),
            "测试手工凭证"));
        assertNotNull(jeId);
        FinJournalEntry je = journalRepo.findById(jeId).orElseThrow();
        assertEquals(JournalStatus.SUBMITTED.code, je.getStatus());
        assertEquals(0, je.getTotalDebit().compareTo(amt), "借方合计应为 1000");
        assertEquals(0, je.getTotalCredit().compareTo(amt), "贷方合计应为 1000");
        assertEquals(2, je.getItems().size(), "应含 2 行明细");

        // (b) 借贷不平 → IAE 阻断
        assertThrows(IllegalArgumentException.class, () -> postingFacade.post(new FinPostingFacade.PostingRequest(
            JournalSourceType.MANUAL_JOURNAL.code, 9002L, "T-MAN-002",
            LocalDate.now(),
            List.of(
                new FinPostingFacade.PostingLine("CASH", amt, null, null, null, null, null, "借银行"),
                new FinPostingFacade.PostingLine("REV", null, new BigDecimal("999.00"), null, null, null, null, "贷收入")
            ),
            "不平凭证")));

        // (c) 反向冲销: 生成等额反向凭证(借贷互换)
        postingFacade.cancel(JournalSourceType.MANUAL_JOURNAL.code, 9001L);
        // 原凭证仍可查(sourceId=9001L 不变)
        List<FinJournalEntry> originals = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.MANUAL_JOURNAL.code, 9001L);
        assertEquals(1, originals.size(), "原凭证应仍为 1 张");
        assertEquals(JournalStatus.SUBMITTED.code, originals.get(0).getStatus(), "原凭证状态不变(仅生成反向凭证)");

        // 反向凭证: sourceId = 原凭证 id
        List<FinJournalEntry> reverses = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.MANUAL_JOURNAL.code, jeId);
        assertEquals(1, reverses.size(), "应存在 1 张反向凭证");
        FinJournalEntry rev = reverses.get(0);
        assertEquals(0, rev.getTotalDebit().compareTo(amt), "反向凭证借方=原贷方 1000");
        assertEquals(0, rev.getTotalCredit().compareTo(amt), "反向凭证贷方=原借方 1000");
    }

    // =================== (3) 销售发票提交 → GL 借应收/贷收入 ===================
    @Test
    void fin3_sales_invoice_submit_gl() {
        seedGlAccounts();
        FinSalesInvoice inv = new FinSalesInvoice();
        inv.setNo("SI-T-003");
        inv.setPostingDate(LocalDate.of(2026, 8, 31));
        inv.setDueDate(LocalDate.of(2026, 9, 30));
        inv.setCustomerId(7001L);
        inv.setCustomerName("测试客户A");
        inv.setStatus(InvoiceStatus.DRAFT.code);
        inv.setTotal(new BigDecimal("5000.00"));
        inv.setGrandTotal(new BigDecimal("5000.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setOutstandingAmount(new BigDecimal("5000.00"));
        inv = salesInvoiceRepo.save(inv);

        String result = salesSubmitHandler.exec(List.of(inv), null,
            new String[]{FinSalesInvoiceSubmitHandler.CODE_SUBMIT});
        assertTrue(result.contains("成功 1"), "销售发票提交应成功,实际=" + result);

        FinSalesInvoice after = salesInvoiceRepo.findById(inv.getId()).orElseThrow();
        assertEquals(InvoiceStatus.SUBMITTED.code, after.getStatus());

        // 验证 GL 凭证: 借 AR / 贷 REV
        List<FinJournalEntry> journals = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.SALES_INVOICE.code, inv.getId());
        assertEquals(1, journals.size(), "应生成 1 张 GL 凭证");
        FinJournalEntry je = journals.get(0);
        assertEquals(JournalStatus.SUBMITTED.code, je.getStatus());
        assertEquals(0, je.getTotalDebit().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, je.getTotalCredit().compareTo(new BigDecimal("5000.00")));
        assertTrue(je.getItems().stream().anyMatch(i ->
            "AR".equals(i.getAccount().getCode()) && i.getDebit().signum() > 0), "凭证应含 AR 借方行");
        assertTrue(je.getItems().stream().anyMatch(i ->
            "REV".equals(i.getAccount().getCode()) && i.getCredit().signum() > 0), "凭证应含 REV 贷方行");
    }

    // =================== (4) 采购发票提交 → GL 借费用/贷应付 ===================
    @Test
    void fin4_purchase_invoice_submit_gl() {
        seedGlAccounts();
        FinPurchaseInvoice inv = new FinPurchaseInvoice();
        inv.setNo("PI-T-004");
        inv.setPostingDate(LocalDate.of(2026, 8, 31));
        inv.setDueDate(LocalDate.of(2026, 9, 30));
        inv.setSupplierId(8001L);
        inv.setSupplierName("测试供应商B");
        inv.setStatus(InvoiceStatus.DRAFT.code);
        inv.setTotal(new BigDecimal("3000.00"));
        inv.setGrandTotal(new BigDecimal("3000.00"));
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setOutstandingAmount(new BigDecimal("3000.00"));
        inv = purchaseInvoiceRepo.save(inv);

        String result = purchaseSubmitHandler.exec(List.of(inv), null,
            new String[]{FinPurchaseInvoiceSubmitHandler.CODE_SUBMIT});
        assertTrue(result.contains("成功 1"), "采购发票提交应成功,实际=" + result);

        FinPurchaseInvoice after = purchaseInvoiceRepo.findById(inv.getId()).orElseThrow();
        assertEquals(InvoiceStatus.SUBMITTED.code, after.getStatus());

        List<FinJournalEntry> journals = journalRepo.findBySourceTypeAndSourceId(
            JournalSourceType.PURCHASE_INVOICE.code, inv.getId());
        assertEquals(1, journals.size(), "应生成 1 张 GL 凭证");
        FinJournalEntry je = journals.get(0);
        assertEquals(0, je.getTotalDebit().compareTo(new BigDecimal("3000.00")));
        assertEquals(0, je.getTotalCredit().compareTo(new BigDecimal("3000.00")));
        assertTrue(je.getItems().stream().anyMatch(i ->
            "EXP".equals(i.getAccount().getCode()) && i.getDebit().signum() > 0), "凭证应含 EXP 借方行");
        assertTrue(je.getItems().stream().anyMatch(i ->
            "AP".equals(i.getAccount().getCode()) && i.getCredit().signum() > 0), "凭证应含 AP 贷方行");
    }

    // =================== (5) 收付款单提交 → GL + 回写 generatedJournalId ===================
    @Test
    void fin5_payment_entry_posting() {
        seedGlAccounts();
        FinPaymentEntry pay = new FinPaymentEntry();
        pay.setNo("PE-T-005");
        pay.setPostingDate(LocalDate.of(2026, 8, 31));
        pay.setPaymentType(PaymentType.RECEIVE.code);
        pay.setPartyType("Customer");
        pay.setPartyId(7001L);
        pay.setPartyName("测试客户A");
        pay.setPaidAmount(new BigDecimal("2000.00"));
        pay.setReferenceNo("BK-2026-005");
        pay.setStatus(PaymentStatus.DRAFT.code);
        pay = paymentRepo.save(pay);

        String result = paymentSubmitHandler.exec(List.of(pay), null,
            new String[]{FinPaymentSubmitHandler.CODE_SUBMIT});
        assertTrue(result.contains("成功 1"), "收付款单提交应成功,实际=" + result);

        FinPaymentEntry after = paymentRepo.findById(pay.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUBMITTED.code, after.getStatus());
        assertNotNull(after.getGeneratedJournalId(), "应回写 generatedJournalId");

        // 验证 GL: 借 CASH / 贷 AR(收款)
        FinJournalEntry je = journalRepo.findById(after.getGeneratedJournalId()).orElseThrow();
        assertEquals(0, je.getTotalDebit().compareTo(new BigDecimal("2000.00")));
        assertEquals(0, je.getTotalCredit().compareTo(new BigDecimal("2000.00")));
        assertTrue(je.getItems().stream().anyMatch(i ->
            "CASH".equals(i.getAccount().getCode()) && i.getDebit().signum() > 0), "凭证应含 CASH 借方行");
        assertTrue(je.getItems().stream().anyMatch(i ->
            "AR".equals(i.getAccount().getCode()) && i.getCredit().signum() > 0), "凭证应含 AR 贷方行");
    }

    // =================== (6) 预算批准 + 成本中心 + 预算使用率 ===================
    @Test
    void fin6_budget_vs_actual() {
        FinCostCenter cc = new FinCostCenter();
        cc.setCode("CC-001");
        cc.setName("总经办成本中心");
        cc.setIsGroup(false);
        cc.setBudgetAllocated(new BigDecimal("100000.00"));
        cc.setStatus(EnableStatus.ENABLED.code);
        cc = costCenterRepo.save(cc);

        FinBudget budget = new FinBudget();
        budget.setCostCenter(cc);
        budget.setFiscalYear("2026");
        budget.setBudgetAmount(new BigDecimal("100000.00"));
        budget.setAllocatedAmount(new BigDecimal("80000.00"));
        budget.setActualAmount(new BigDecimal("65000.00"));
        budget.setStatus(BudgetStatus.DRAFT.code);
        budget = budgetRepo.save(budget);

        // 行按钮批准 → APPROVED
        String result = budgetApproveHandler.exec(List.of(budget), null,
            new String[]{FinBudgetApproveHandler.CODE_APPROVE});
        assertTrue(result.contains("成功 1"), "预算批准应成功,实际=" + result);

        FinBudget after = budgetRepo.findById(budget.getId()).orElseThrow();
        assertEquals(BudgetStatus.APPROVED.code, after.getStatus());

        // 预算 vs 实际: actual(65000) / budget(100000) = 65%
        BigDecimal usage = after.getActualAmount()
            .divide(after.getBudgetAmount(), 4, RoundingMode.HALF_UP);
        assertEquals(0, usage.compareTo(new BigDecimal("0.6500")), "预算使用率应为 65%");
        assertTrue(usage.compareTo(BigDecimal.ONE) < 0, "未超预算");
    }
}
