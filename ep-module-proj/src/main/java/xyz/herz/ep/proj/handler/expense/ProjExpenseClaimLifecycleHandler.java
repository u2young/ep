package xyz.herz.ep.proj.handler.expense;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingLine;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingRequest;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;
import xyz.herz.ep.proj.entity.cashflow.ProjCashFlow;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaim;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaimItem;
import xyz.herz.ep.proj.enums.ProjDictEnums.ActivityCostType;
import xyz.herz.ep.proj.enums.ProjDictEnums.CashFlowType;
import xyz.herz.ep.proj.enums.ProjDictEnums.ExpenseClaimStatus;
import xyz.herz.ep.proj.jpa.activitycost.ProjActivityCostRepository;
import xyz.herz.ep.proj.jpa.cashflow.ProjCashFlowRepository;
import xyz.herz.ep.proj.jpa.project.ProjProjectRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用报销生命周期行按钮处理器(提交/批准/拒绝/入账,四合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>SUBMIT:DRAFT(0) → SUBMITTED(1),校验 items,回写 totalAmount = sum(items.amount)</li>
 *   <li>APPROVE:SUBMITTED(1) → APPROVED(2),回写 sanctionedAmount(默认 = totalAmount)</li>
 *   <li>REJECT:SUBMITTED(1) → REJECTED(3)</li>
 *   <li>POST:APPROVED(2) → POSTED(4),插入 ProjActivityCost(EXPENSE) + ProjCashFlow(OUTFLOW) +
 *       累加 ProjProject.totalCost + 触发 GL(I-9:借 EXP / 贷 AP = sanctionedAmount)</li>
 * </ul>
 * GL 通过 FinPostingFacade 触发(可选注入)。
 *
 * <p>GL 科目约定(测试用 fin FinSmokeTests 种子):EXP=费用 / AP=应付。
 */
@Component
public class ProjExpenseClaimLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "proj.expense.submit";
    public static final String CODE_APPROVE = "proj.expense.approve";
    public static final String CODE_REJECT = "proj.expense.reject";
    public static final String CODE_POST = "proj.expense.post";

    private static final String ACCT_EXP = "EXP";
    private static final String ACCT_AP = "AP";

    @PersistenceContext private EntityManager em;
    private final ProjActivityCostRepository activityCostRepo;
    private final ProjCashFlowRepository cashFlowRepo;
    private final ProjProjectRepository projectRepo;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    public ProjExpenseClaimLifecycleHandler(ProjActivityCostRepository activityCostRepo,
                                            ProjCashFlowRepository cashFlowRepo,
                                            ProjProjectRepository projectRepo) {
        this.activityCostRepo = activityCostRepo;
        this.cashFlowRepo = cashFlowRepo;
        this.projectRepo = projectRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ProjExpenseClaim doc)) {
                fail++; sb.append("仅支持报销单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_APPROVE -> applyApprove(doc);
                    case CODE_REJECT -> applyReject(doc);
                    case CODE_POST -> applyPost(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("报销#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applySubmit(ProjExpenseClaim doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ExpenseClaimStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可提交,当前状态=" + st);
        }
        if (doc.getItems() == null || doc.getItems().isEmpty()) {
            throw new IllegalStateException("报销单至少 1 行明细");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ProjExpenseClaimItem it : doc.getItems()) {
            if (it.getAmount() == null || it.getAmount().signum() < 0) {
                throw new IllegalStateException("明细金额不可为负");
            }
            total = total.add(it.getAmount());
        }
        doc.setTotalAmount(total);
        doc.setStatus(ExpenseClaimStatus.SUBMITTED.code);
    }

    private void applyApprove(ProjExpenseClaim doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ExpenseClaimStatus.SUBMITTED.code) {
            throw new IllegalStateException("只有已提交状态可批准,当前状态=" + st);
        }
        // 核准金额默认 = 报销总额(后续可由审批人调整)
        BigDecimal sanctioned = doc.getTotalAmount() == null ? BigDecimal.ZERO : doc.getTotalAmount();
        doc.setSanctionedAmount(sanctioned);
        doc.setStatus(ExpenseClaimStatus.APPROVED.code);
    }

    private void applyReject(ProjExpenseClaim doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ExpenseClaimStatus.SUBMITTED.code) {
            throw new IllegalStateException("只有已提交状态可拒绝,当前状态=" + st);
        }
        doc.setStatus(ExpenseClaimStatus.REJECTED.code);
    }

    private void applyPost(ProjExpenseClaim doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ExpenseClaimStatus.APPROVED.code) {
            throw new IllegalStateException("只有已批准状态可入账,当前状态=" + st);
        }
        if (doc.getProject() == null || doc.getProject().getId() == null) {
            throw new IllegalStateException("报销单必须关联项目");
        }
        BigDecimal amt = doc.getSanctionedAmount() == null ? BigDecimal.ZERO : doc.getSanctionedAmount();
        if (amt.signum() <= 0) {
            throw new IllegalStateException("核准金额必须 > 0");
        }
        doc.setStatus(ExpenseClaimStatus.POSTED.code);

        // 1. 插入活动成本(EXPENSE)
        ProjActivityCost ac = new ProjActivityCost();
        ac.setProject(doc.getProject());
        ac.setTask(doc.getTask());
        ac.setCostType(ActivityCostType.EXPENSE.code);
        ac.setAmount(amt);
        ac.setPostingDate(LocalDate.now());
        ac.setSourceType("ExpenseClaim");
        ac.setSourceId(doc.getId());
        activityCostRepo.save(ac);

        // 2. 插入现金流(OUTFLOW)
        ProjCashFlow cf = new ProjCashFlow();
        cf.setProject(doc.getProject());
        cf.setFlowType(CashFlowType.OUTFLOW.code);
        cf.setAmount(amt);
        cf.setPostingDate(LocalDate.now());
        cf.setSourceType("ExpenseClaim");
        cf.setSourceId(doc.getId());
        cf.setSourceNo(doc.getNo());
        cf.setRemark("费用报销支出 " + doc.getNo());
        cashFlowRepo.save(cf);

        // 3. 累加项目 totalCost
        projectRepo.findById(doc.getProject().getId()).ifPresent(p -> {
            BigDecimal cur = p.getTotalCost() == null ? BigDecimal.ZERO : p.getTotalCost();
            p.setTotalCost(cur.add(amt));
            em.merge(p);
        });

        // 4. 触发 GL(I-9):借 EXP / 贷 AP = amt
        if (postingFacade != null) {
            PostingRequest req = new PostingRequest(
                "ProjectExpense", doc.getId(), doc.getNo(), LocalDate.now(),
                List.of(
                    new PostingLine(ACCT_EXP, amt, null, "Employee",
                        doc.getEmployeeId(), doc.getEmployeeName(),
                        doc.getProject().getCostCenterId(), "项目费用入账"),
                    new PostingLine(ACCT_AP, null, amt, "Employee",
                        doc.getEmployeeId(), doc.getEmployeeName(),
                        doc.getProject().getCostCenterId(), "应付员工费用")),
                "费用报销入账 " + doc.getNo());
            postingFacade.post(req);
        }
    }
}
