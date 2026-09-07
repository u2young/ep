package xyz.herz.ep.sup.handler.issue;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.EruptButtonHandler;
import xyz.herz.ep.sup.entity.assignment.SupIssueAssignment;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;
import xyz.herz.ep.sup.jpa.issue.SupIssueRepository;
import xyz.herz.ep.sup.jpa.settings.SupSupportSettingsRepository;

import java.time.LocalDateTime;

/**
 * 工单自动分派按钮处理器(参考 ERPNext Issue Auto Assignment)。
 * <p>用法:在 {@link SupIssue} 编辑页点「自动分派」按钮,
 * 根据 {@link SupSupportSettings} 单例的默认客服(autoAssignEnabled + defaultAgentId)分派本工单,
 * 同时追加一条 {@link SupIssueAssignment} 分派历史。
 *
 * <p>业务校验:
 * <ul>
 *   <li>settings 不存在 → 提示未配置;</li>
 *   <li>autoAssignEnabled=false → 提示未开启;</li>
 *   <li>defaultAgentId=null → 提示未配置默认客服。</li>
 * </ul>
 */
@Component
public class SupIssueAutoAssignButtonHandler implements EruptButtonHandler<SupIssue> {

    private final SupSupportSettingsRepository settingsRepo;
    private final SupIssueRepository issueRepo;

    public SupIssueAutoAssignButtonHandler(SupSupportSettingsRepository settingsRepo,
                                           SupIssueRepository issueRepo) {
        this.settingsRepo = settingsRepo;
        this.issueRepo = issueRepo;
    }

    @Override
    public String click(SupIssue issue, String[] params) {
        return exec(issue);
    }

    /**
     * 核心逻辑(可直接被测试调用,跳过 click 接口层)。
     * @param issue 工单(必须已持久化)
     * @return 结果提示文案
     */
    @Transactional
    public String exec(SupIssue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("工单不能为空");
        }
        SupSupportSettings settings = settingsRepo.findFirstByOrderByIdAsc().orElse(null);
        if (settings == null) {
            return "未配置服务支持设置(SupSupportSettings),无法自动分派";
        }
        if (!Boolean.TRUE.equals(settings.getAutoAssignEnabled())) {
            return "自动分派未开启(SupSupportSettings.autoAssignEnabled=false)";
        }
        if (settings.getDefaultAgentId() == null) {
            return "未配置默认客服(SupSupportSettings.defaultAgentId),无法自动分派";
        }

        issue.setAssigneeId(settings.getDefaultAgentId());
        issue.setAssigneeName(settings.getDefaultAgentName());

        SupIssueAssignment a = new SupIssueAssignment();
        a.setIssue(issue);
        a.setAssigneeId(settings.getDefaultAgentId());
        a.setAssigneeName(settings.getDefaultAgentName());
        a.setAssignedAt(LocalDateTime.now());
        a.setNote("自动分派(按服务支持设置)");
        if (issue.getAssignments() == null) {
            issue.setAssignments(new java.util.ArrayList<>());
        }
        issue.getAssignments().add(a);

        issueRepo.save(issue);
        return "已自动分派给:" + settings.getDefaultAgentName()
                + "(id=" + settings.getDefaultAgentId() + ")";
    }
}
