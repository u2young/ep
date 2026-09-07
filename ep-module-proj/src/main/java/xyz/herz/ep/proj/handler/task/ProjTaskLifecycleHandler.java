package xyz.herz.ep.proj.handler.task;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.entity.task.ProjTask;
import xyz.herz.ep.proj.enums.ProjDictEnums.ProjectStatus;
import xyz.herz.ep.proj.enums.ProjDictEnums.TaskStatus;
import xyz.herz.ep.proj.jpa.project.ProjProjectRepository;
import xyz.herz.ep.proj.jpa.task.ProjTaskRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 任务生命周期行按钮处理器(开工/完成,二合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>START:NOT_STARTED(0) → IN_PROGRESS(1),startDate=now</li>
 *   <li>COMPLETE:IN_PROGRESS(1) → COMPLETED(2),progress=100,endDate=now +
 *       刷新父任务 progress(= 子任务平均进度)+ 项目 percentComplete(= 完成任务数/总任务数)</li>
 * </ul>
 */
@Component
public class ProjTaskLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_START = "proj.task.start";
    public static final String CODE_COMPLETE = "proj.task.complete";

    @PersistenceContext private EntityManager em;
    private final ProjTaskRepository taskRepo;
    private final ProjProjectRepository projectRepo;

    public ProjTaskLifecycleHandler(ProjTaskRepository taskRepo, ProjProjectRepository projectRepo) {
        this.taskRepo = taskRepo;
        this.projectRepo = projectRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_START;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ProjTask doc)) {
                fail++; sb.append("仅支持任务; "); continue;
            }
            try {
                switch (code) {
                    case CODE_START -> applyStart(doc);
                    case CODE_COMPLETE -> applyComplete(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("任务#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyStart(ProjTask doc) {
        Integer st = doc.getStatus();
        if (st == null || st != TaskStatus.NOT_STARTED.code) {
            throw new IllegalStateException("只有未开始状态可开工,当前状态=" + st);
        }
        if (doc.getProject() == null || doc.getProject().getId() == null) {
            throw new IllegalStateException("任务必须关联项目");
        }
        // 项目须为进行中才允许任务开工
        ProjProject proj = projectRepo.findById(doc.getProject().getId()).orElse(null);
        if (proj == null || proj.getStatus() == null
                || proj.getStatus() != ProjectStatus.IN_PROGRESS.code) {
            throw new IllegalStateException("关联项目非进行中状态,不可开工任务");
        }
        doc.setStatus(TaskStatus.IN_PROGRESS.code);
        if (doc.getStartDate() == null) doc.setStartDate(LocalDate.now());
    }

    private void applyComplete(ProjTask doc) {
        Integer st = doc.getStatus();
        if (st == null || st != TaskStatus.IN_PROGRESS.code) {
            throw new IllegalStateException("只有进行中状态可完成,当前状态=" + st);
        }
        doc.setStatus(TaskStatus.COMPLETED.code);
        doc.setProgress(new BigDecimal("100.00"));
        if (doc.getEndDate() == null) doc.setEndDate(LocalDate.now());
        // 刷新父任务进度(= 子任务平均进度)
        if (doc.getParentTask() != null && doc.getParentTask().getId() != null) {
            refreshParentProgress(doc.getParentTask().getId());
        }
        // 刷新项目 percentComplete(= 完成任务数/总任务数)
        if (doc.getProject() != null && doc.getProject().getId() != null) {
            refreshProjectPercent(doc.getProject().getId());
        }
    }

    private void refreshParentProgress(Long parentId) {
        List<ProjTask> children = taskRepo.findByParentTaskId(parentId);
        if (children == null || children.isEmpty()) return;
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (ProjTask c : children) {
            if (c.getProgress() != null) { sum = sum.add(c.getProgress()); n++; }
        }
        if (n == 0) return;
        BigDecimal avg = sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        ProjTask parent = taskRepo.findById(parentId).orElse(null);
        if (parent != null) {
            parent.setProgress(avg);
            // 父任务全部子完成则父也完成
            boolean allDone = children.stream().allMatch(c ->
                c.getStatus() != null && c.getStatus() == TaskStatus.COMPLETED.code);
            if (allDone) {
                parent.setStatus(TaskStatus.COMPLETED.code);
                if (parent.getEndDate() == null) parent.setEndDate(LocalDate.now());
            }
            em.merge(parent);
        }
    }

    private void refreshProjectPercent(Long projectId) {
        List<ProjTask> tasks = taskRepo.findByProjectId(projectId);
        if (tasks == null || tasks.isEmpty()) return;
        int total = tasks.size();
        long done = tasks.stream().filter(t ->
            t.getStatus() != null && t.getStatus() == TaskStatus.COMPLETED.code).count();
        BigDecimal pct = BigDecimal.valueOf(done)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        ProjProject proj = projectRepo.findById(projectId).orElse(null);
        if (proj != null) {
            proj.setPercentComplete(pct);
            em.merge(proj);
        }
    }
}
