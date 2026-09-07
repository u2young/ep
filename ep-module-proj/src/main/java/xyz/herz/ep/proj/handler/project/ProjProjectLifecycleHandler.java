package xyz.herz.ep.proj.handler.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingLine;
import xyz.herz.ep.fin.facade.FinPostingFacade.PostingRequest;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.enums.ProjDictEnums.ProjectStatus;
import xyz.herz.ep.proj.jpa.activitycost.ProjActivityCostRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目生命周期行按钮处理器(立项/开工/完工/暂停/取消,五合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>APPROVE:DRAFT(0) → APPROVED(1),校验客户/预计收入,回写 grossMargin</li>
 *   <li>START:APPROVED(1) → IN_PROGRESS(2),记录 actualStart</li>
 *   <li>COMPLETE:IN_PROGRESS(2)/ON_HOLD(3) → COMPLETED(4),actualEnd + percentComplete=100 +
 *       重算 totalCost(= sum activity_cost) + 收入确认 GL(I-10:借 AR / 贷 REV = totalRevenue)</li>
 *   <li>PAUSE:IN_PROGRESS(2) → ON_HOLD(3)</li>
 *   <li>CANCEL:DRAFT/APPROVED/IN_PROGRESS/ON_HOLD → CANCELLED(5);已完工不可取消</li>
 * </ul>
 * GL 通过 FinPostingFacade 触发(可选注入:fin 不在 classpath 时跳过,保持 proj 独立可测)。
 *
 * <p>GL 科目约定(测试用 fin FinSmokeTests 种子):AR=应收账款 / REV=主营业务收入。
 */
@Component
public class ProjProjectLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_APPROVE = "proj.project.approve";
    public static final String CODE_START = "proj.project.start";
    public static final String CODE_COMPLETE = "proj.project.complete";
    public static final String CODE_PAUSE = "proj.project.pause";
    public static final String CODE_CANCEL = "proj.project.cancel";

    /** GL 科目代码:应收账款(借方)/ 主营业务收入(贷方)。 */
    private static final String ACCT_AR = "AR";
    private static final String ACCT_REV = "REV";

    @PersistenceContext private EntityManager em;
    private final ProjActivityCostRepository activityCostRepo;

    /** 可选注入:fin 不在 classpath 时为 null,跳过 GL。 */
    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    public ProjProjectLifecycleHandler(ProjActivityCostRepository activityCostRepo) {
        this.activityCostRepo = activityCostRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_APPROVE;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ProjProject doc)) {
                fail++; sb.append("仅支持项目; "); continue;
            }
            try {
                switch (code) {
                    case CODE_APPROVE -> applyApprove(doc);
                    case CODE_START -> applyStart(doc);
                    case CODE_COMPLETE -> applyComplete(doc);
                    case CODE_PAUSE -> applyPause(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("项目#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyApprove(ProjProject doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ProjectStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可立项,当前状态=" + st);
        }
        if (doc.getTotalRevenue() == null || doc.getTotalRevenue().signum() < 0) {
            throw new IllegalStateException("预计收入不可为负");
        }
        recomputeMargins(doc);
        doc.setStatus(ProjectStatus.APPROVED.code);
    }

    private void applyStart(ProjProject doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ProjectStatus.APPROVED.code) {
            throw new IllegalStateException("只有已立项状态可开工,当前状态=" + st);
        }
        doc.setStatus(ProjectStatus.IN_PROGRESS.code);
        doc.setActualStart(LocalDateTime.now());
    }

    private void applyComplete(ProjProject doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != ProjectStatus.IN_PROGRESS.code && st != ProjectStatus.ON_HOLD.code)) {
            throw new IllegalStateException("只有进行中/暂停状态可完工,当前状态=" + st);
        }
        // 重算累计成本(= sum activity_cost where project)
        recomputeMargins(doc);
        doc.setPercentComplete(new BigDecimal("100.00"));
        doc.setActualEnd(LocalDateTime.now());
        doc.setStatus(ProjectStatus.COMPLETED.code);

        // I-10 收入确认 GL:借 AR / 贷 REV = totalRevenue(收入 > 0 才过账)
        if (postingFacade != null && doc.getTotalRevenue() != null
                && doc.getTotalRevenue().signum() > 0) {
            BigDecimal amt = doc.getTotalRevenue();
            PostingRequest req = new PostingRequest(
                "ProjectRevenue", doc.getId(), doc.getNo(), LocalDate.now(),
                List.of(
                    new PostingLine(ACCT_AR, amt, null, "Customer",
                        doc.getCustomerId(), doc.getCustomerName(),
                        doc.getCostCenterId(), "项目收入确认-应收"),
                    new PostingLine(ACCT_REV, null, amt, "Customer",
                        doc.getCustomerId(), doc.getCustomerName(),
                        doc.getCostCenterId(), "项目收入确认-收入")),
                "项目完工收入确认 " + doc.getNo());
            postingFacade.post(req);
        }
    }

    private void applyPause(ProjProject doc) {
        Integer st = doc.getStatus();
        if (st == null || st != ProjectStatus.IN_PROGRESS.code) {
            throw new IllegalStateException("只有进行中状态可暂停,当前状态=" + st);
        }
        doc.setStatus(ProjectStatus.ON_HOLD.code);
    }

    private void applyCancel(ProjProject doc) {
        Integer st = doc.getStatus();
        if (st == null || st == ProjectStatus.COMPLETED.code) {
            throw new IllegalStateException("已完工项目不可取消,当前状态=" + st);
        }
        if (st == ProjectStatus.CANCELLED.code) {
            throw new IllegalStateException("项目已取消");
        }
        doc.setStatus(ProjectStatus.CANCELLED.code);
    }

    /** 重算 totalCost(= sum activity_cost) + grossMargin(= totalRevenue - totalCost)。 */
    private void recomputeMargins(ProjProject doc) {
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ProjActivityCost> costs = activityCostRepo.findByProjectId(doc.getId());
        if (costs != null) {
            for (ProjActivityCost c : costs) {
                if (c.getAmount() != null) totalCost = totalCost.add(c.getAmount());
            }
        }
        doc.setTotalCost(totalCost);
        BigDecimal rev = doc.getTotalRevenue() == null ? BigDecimal.ZERO : doc.getTotalRevenue();
        doc.setGrossMargin(rev.subtract(totalCost));
    }
}
