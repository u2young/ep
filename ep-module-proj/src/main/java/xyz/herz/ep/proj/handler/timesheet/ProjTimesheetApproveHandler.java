package xyz.herz.ep.proj.handler.timesheet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingLine;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingRequest;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.entity.timesheet.ProjTimesheet;
import xyz.herz.ep.proj.enums.ProjDictEnums.ActivityCostType;
import xyz.herz.ep.proj.enums.ProjDictEnums.BillingStatus;
import xyz.herz.ep.proj.jpa.activitycost.ProjActivityCostRepository;
import xyz.herz.ep.proj.jpa.project.ProjProjectRepository;
import xyz.herz.ep.proj.jpa.task.ProjTaskRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时表审批行按钮处理器(0 未计费 → 1 已计费)。
 * <p>审批时:
 * <ol>
 *   <li>计算 hours = Duration 小时 / cost = hours * billingRate,回写工时表</li>
 *   <li>插入 ProjActivityCost(costType=LABOR)活动成本行</li>
 *   <li>累加 ProjTask.actualTime(+= hours)</li>
 *   <li>累加 ProjProject.totalCost(+= cost)</li>
 *   <li>触发 GL(I-11:借 EXP 项目成本 / 贷 AP 应付薪酬 = cost)</li>
 * </ol>
 * GL 通过 FinPostingFacade 触发(可选注入:fin 不在 classpath 时跳过)。
 *
 * <p>GL 科目约定(测试用 fin FinSmokeTests 种子):EXP=费用 / AP=应付。
 */
@Component
public class ProjTimesheetApproveHandler implements OperationHandler<Object, Object> {

    public static final String CODE_APPROVE = "proj.timesheet.approve";

    /** GL 科目代码:项目成本(借方)/ 应付薪酬(贷方)。 */
    private static final String ACCT_EXP = "EXP";
    private static final String ACCT_AP = "AP";

    @PersistenceContext private EntityManager em;
    private final ProjActivityCostRepository activityCostRepo;
    private final ProjTaskRepository taskRepo;
    private final ProjProjectRepository projectRepo;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    public ProjTimesheetApproveHandler(ProjActivityCostRepository activityCostRepo,
                                       ProjTaskRepository taskRepo,
                                       ProjProjectRepository projectRepo) {
        this.activityCostRepo = activityCostRepo;
        this.taskRepo = taskRepo;
        this.projectRepo = projectRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ProjTimesheet doc)) {
                fail++; sb.append("仅支持工时表; "); continue;
            }
            try {
                applyApprove(doc);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("工时#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_APPROVE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyApprove(ProjTimesheet doc) {
        Integer bs = doc.getBillingStatus();
        if (bs == null || bs != BillingStatus.UNBILLED.code) {
            throw new IllegalStateException("只有未计费状态可审批,当前状态=" + bs);
        }
        if (doc.getFromTime() == null || doc.getToTime() == null
                || doc.getToTime().isBefore(doc.getFromTime())) {
            throw new IllegalStateException("时间区间非法(fromTime/toTime)");
        }
        if (doc.getProject() == null || doc.getProject().getId() == null) {
            throw new IllegalStateException("工时必须关联项目");
        }

        // 1. 计算 hours / cost
        long seconds = Duration.between(doc.getFromTime(), doc.getToTime()).getSeconds();
        BigDecimal hours = BigDecimal.valueOf(seconds)
            .divide(new BigDecimal("3600"), 4, RoundingMode.HALF_UP);
        BigDecimal rate = doc.getBillingRate() == null ? BigDecimal.ZERO : doc.getBillingRate();
        BigDecimal cost = hours.multiply(rate).setScale(6, RoundingMode.HALF_UP);
        doc.setHours(hours);
        doc.setCost(cost);
        doc.setBillingStatus(BillingStatus.BILLED.code);

        // 2. 插入活动成本(LABOR)
        ProjActivityCost ac = new ProjActivityCost();
        ac.setProject(doc.getProject());
        ac.setTask(doc.getTask());
        ac.setCostType(ActivityCostType.LABOR.code);
        ac.setAmount(cost);
        ac.setPostingDate(LocalDate.now());
        ac.setSourceType("Timesheet");
        ac.setSourceId(doc.getId());
        activityCostRepo.save(ac);

        // 3. 累加任务 actualTime
        if (doc.getTask() != null && doc.getTask().getId() != null) {
            taskRepo.findById(doc.getTask().getId()).ifPresent(t -> {
                BigDecimal cur = t.getActualTime() == null ? BigDecimal.ZERO : t.getActualTime();
                t.setActualTime(cur.add(hours));
                em.merge(t);
            });
        }

        // 4. 累加项目 totalCost
        projectRepo.findById(doc.getProject().getId()).ifPresent(p -> {
            BigDecimal cur = p.getTotalCost() == null ? BigDecimal.ZERO : p.getTotalCost();
            p.setTotalCost(cur.add(cost));
            em.merge(p);
        });

        // 5. 触发 GL(I-11):借 EXP / 贷 AP = cost
        if (postingFacade != null && cost.signum() > 0) {
            PostingRequest req = new PostingRequest(
                "ProjectExpense", doc.getId(), "TS-" + doc.getId(), LocalDate.now(),
                List.of(
                    new PostingLine(ACCT_EXP, cost, null, "Employee",
                        doc.getEmployeeId(), doc.getEmployeeName(),
                        doc.getProject().getCostCenterId(), "项目人工成本"),
                    new PostingLine(ACCT_AP, null, cost, "Employee",
                        doc.getEmployeeId(), doc.getEmployeeName(),
                        doc.getProject().getCostCenterId(), "应付员工薪酬")),
                "工时审批入账 " + doc.getEmployeeName());
            postingFacade.post(req);
        }
    }
}
