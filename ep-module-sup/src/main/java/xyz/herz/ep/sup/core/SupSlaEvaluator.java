package xyz.herz.ep.sup.core;

import org.springframework.stereotype.Service;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sup.entity.priority.SupIssuePriority;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;
import xyz.herz.ep.sup.entity.sla.SupServiceLevelAgreement;
import xyz.herz.ep.sup.enums.SupDictEnums.IssuePriority;
import xyz.herz.ep.sup.jpa.priority.SupIssuePriorityRepository;
import xyz.herz.ep.sup.jpa.settings.SupSupportSettingsRepository;
import xyz.herz.ep.sup.jpa.sla.SupServiceLevelAgreementRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SLA 引擎(参考 ERPNext SLA 计算)。
 * <p>核心职责:
 * <ol>
 *   <li>{@link #applyDeadlines(SupIssue, LocalDateTime)} 根据 SLA/优先级/默认配置,
 *       从 baseline 起算 responseBy(响应截止)/resolutionBy(解决截止)并回写工单。</li>
 *   <li>{@link #computeFulfillment(SupIssue)} 计算工单 SLA 是否达成:
 *       firstResponseAt ≤ responseBy 且 resolvedAt ≤ resolutionBy。</li>
 * </ol>
 *
 * <p>时长解析优先级:
 * 1) issue.sla 直接指定 → 2) SupSupportSettings.defaultSlaId →
 * 3) SupIssuePriority 表按优先级 → 4) 枚举硬编码 fallback(60/480 分钟)。
 */
@Service
public class SupSlaEvaluator {

    private static final int[] FALLBACK = new int[]{ 60, 480 };

    private final SupServiceLevelAgreementRepository slaRepo;
    private final SupIssuePriorityRepository priorityRepo;
    private final SupSupportSettingsRepository settingsRepo;

    public SupSlaEvaluator(SupServiceLevelAgreementRepository slaRepo,
                           SupIssuePriorityRepository priorityRepo,
                           SupSupportSettingsRepository settingsRepo) {
        this.slaRepo = slaRepo;
        this.priorityRepo = priorityRepo;
        this.settingsRepo = settingsRepo;
    }

    /**
     * 从 baseline 起算 responseBy/resolutionBy 并写入工单。
     * @param issue 工单(若已指定 sla 则用 sla 时长,否则查默认配置/优先级表)
     * @param baseline 计算基线(通常为工单创建时间或重开时间)
     */
    public void applyDeadlines(SupIssue issue, LocalDateTime baseline) {
        if (issue == null || baseline == null) return;
        int[] times = resolveTimes(issue);
        issue.setResponseBy(baseline.plusMinutes(times[0]));
        issue.setResolutionBy(baseline.plusMinutes(times[1]));
    }

    /**
     * 计算工单 SLA 达成情况(响应/解决均在截止内)。
     * @return true=达成;false=违约;null=截止时间未设(无法判定)
     */
    public Boolean computeFulfillment(SupIssue issue) {
        if (issue == null || issue.getResponseBy() == null || issue.getResolutionBy() == null) {
            return null;
        }
        boolean respOk = issue.getFirstResponseAt() == null
                || !issue.getFirstResponseAt().isAfter(issue.getResponseBy());
        boolean resOk = issue.getResolvedAt() == null
                || !issue.getResolvedAt().isAfter(issue.getResolutionBy());
        return respOk && resOk;
    }

    private int[] resolveTimes(SupIssue issue) {
        // 1) issue.sla 直接指定
        if (issue.getSla() != null && issue.getSla().getId() != null) {
            Optional<SupServiceLevelAgreement> sla = slaRepo.findById(issue.getSla().getId());
            if (sla.isPresent()) {
                return new int[]{ sla.get().getResponseTimeMins(), sla.get().getResolutionTimeMins() };
            }
        }
        // 2) settings.defaultSlaId
        Optional<SupSupportSettings> settings = settingsRepo.findFirstByOrderByIdAsc();
        if (settings.isPresent() && settings.get().getDefaultSlaId() != null) {
            Optional<SupServiceLevelAgreement> defaultSla = slaRepo.findById(settings.get().getDefaultSlaId());
            if (defaultSla.isPresent()) {
                return new int[]{ defaultSla.get().getResponseTimeMins(),
                                  defaultSla.get().getResolutionTimeMins() };
            }
        }
        // 3) SupIssuePriority 表(按 issue.priority → IssuePriority.name → findByCode)
        Integer p = issue.getPriority();
        if (p != null) {
            for (IssuePriority ip : IssuePriority.values()) {
                if (ip.code == p) {
                    Optional<SupIssuePriority> priority = priorityRepo.findByCode(ip.name());
                    if (priority.isPresent()) {
                        return new int[]{ priority.get().getDefaultSlaResponseMins(),
                                          priority.get().getDefaultSlaResolutionMins() };
                    }
                    break;
                }
            }
        }
        // 4) 枚举硬编码 fallback
        return FALLBACK;
    }
}
