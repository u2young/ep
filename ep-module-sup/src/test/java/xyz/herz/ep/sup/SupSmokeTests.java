package xyz.herz.ep.sup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import xyz.herz.ep.sup.core.SupSlaEvaluator;
import xyz.herz.ep.sup.entity.assignment.SupIssueAssignment;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.sup.entity.kb.SupKnowledgeBase;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;
import xyz.herz.ep.sup.entity.sla.SupServiceLevelAgreement;
import xyz.herz.ep.sup.enums.SupDictEnums.EnableStatus;
import xyz.herz.ep.sup.enums.SupDictEnums.IssuePriority;
import xyz.herz.ep.sup.enums.SupDictEnums.IssueStatus;
import xyz.herz.ep.sup.enums.SupDictEnums.KbStatus;
import xyz.herz.ep.sup.handler.issue.SupIssueAutoAssignButtonHandler;
import xyz.herz.ep.sup.handler.issue.SupIssueLifecycleHandler;
import xyz.herz.ep.sup.handler.kb.SupKbPublishHandler;
import xyz.herz.ep.sup.jpa.issue.SupIssueRepository;
import xyz.herz.ep.sup.jpa.kb.SupKnowledgeBaseRepository;
import xyz.herz.ep.sup.jpa.settings.SupSupportSettingsRepository;
import xyz.herz.ep.sup.jpa.sla.SupServiceLevelAgreementRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 服务支持模块冒烟测试(参考 ERPNext Support DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-D 验收矩阵:
 * <ol>
 *   <li>Sup1 工单生命周期:回复/解决/关闭/重开/取消状态机 + 已关闭不可取消</li>
 *   <li>Sup2 SLA 达成:在截止内响应+解决 → slaFulfilled=true</li>
 *   <li>Sup3 SLA 违约:超过截止时间 → slaFulfilled=false</li>
 *   <li>Sup4 自动分派:按 SupSupportSettings 默认客服分派 + 分派历史</li>
 *   <li>Sup5 知识库发布归档:草稿→发布→归档状态机</li>
 *   <li>Sup6 客户关联:SupIssue.customer REF CrmCustomer(I-12 跨模块)</li>
 * </ol>
 *
 * <p>分派人/客服用快照(assigneeId+assigneeName),不 REF UPMS,保持 sup 独立可测。
 * SLA 截止由 {@link SupSlaEvaluator} 计算;SLA 达成判定 firstResponseAt ≤ responseBy 且 resolvedAt ≤ resolutionBy。
 */
@SpringBootTest(classes = SupTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class SupSmokeTests {

    @Autowired SupIssueRepository issueRepo;
    @Autowired SupServiceLevelAgreementRepository slaRepo;
    @Autowired SupKnowledgeBaseRepository kbRepo;
    @Autowired SupSupportSettingsRepository settingsRepo;
    @Autowired SupIssueLifecycleHandler issueLifecycle;
    @Autowired SupIssueAutoAssignButtonHandler autoAssign;
    @Autowired SupKbPublishHandler kbPublish;
    @Autowired SupSlaEvaluator slaEvaluator;
    @Autowired CrmCustomerRepository customerRepo;

    /** 种 SLA(宽松时长,用于「达成」场景)。 */
    private SupServiceLevelAgreement seedSla(int responseMins, int resolutionMins) {
        SupServiceLevelAgreement sla = new SupServiceLevelAgreement();
        sla.setName("测试SLA-" + System.nanoTime());
        sla.setResponseTimeMins(responseMins);
        sla.setResolutionTimeMins(resolutionMins);
        sla.setStatus(EnableStatus.ENABLED.code);
        sla.setPriority(IssuePriority.MEDIUM.code);
        return slaRepo.save(sla);
    }

    private SupIssue basicIssue(String subject, SupServiceLevelAgreement sla) {
        SupIssue i = new SupIssue();
        i.setSubject(subject);
        i.setPriority(IssuePriority.MEDIUM.code);
        i.setRaisedBy("测试提交人");
        i.setRaisedById(5001L);
        i.setSla(sla);
        i.setStatus(IssueStatus.OPEN.code);
        return issueRepo.save(i);
    }

    // =================== (1) 工单生命周期:回复/解决/关闭/重开/取消 ===================
    @Test
    void sup1_issue_lifecycle() {
        SupServiceLevelAgreement sla = seedSla(1440, 2880);
        SupIssue i = basicIssue("TICKET-001", sla);
        slaEvaluator.applyDeadlines(i, i.getCreateTime());
        issueRepo.save(i);

        // 回复:OPEN → REPLIED + firstResponseAt
        String r1 = issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_REPLY});
        assertTrue(r1.contains("成功 1"), "回复应成功,实际=" + r1);
        SupIssue replied = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.REPLIED.code, replied.getStatus());
        assertNotNull(replied.getFirstResponseAt(), "回复应记 firstResponseAt");

        // 解决:REPLIED → RESOLVED + resolvedAt + slaFulfilled
        String r2 = issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_RESOLVE});
        assertTrue(r2.contains("成功 1"), "解决应成功,实际=" + r2);
        SupIssue resolved = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.RESOLVED.code, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        assertNotNull(resolved.getSlaFulfilled(), "解决后应计算 SLA 达成");

        // 关闭:RESOLVED → CLOSED + closedAt
        String r3 = issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_CLOSE});
        assertTrue(r3.contains("成功 1"), "关闭应成功,实际=" + r3);
        SupIssue closed = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.CLOSED.code, closed.getStatus());
        assertNotNull(closed.getClosedAt());

        // 重开:CLOSED → REOPENED + 重算 SLA 截止 + 清 resolvedAt/closedAt/slaFulfilled
        String r4 = issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_REOPEN});
        assertTrue(r4.contains("成功 1"), "重开应成功,实际=" + r4);
        SupIssue reopened = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.REOPENED.code, reopened.getStatus());
        assertNull(reopened.getResolvedAt(), "重开应清空 resolvedAt");
        assertNull(reopened.getClosedAt(), "重开应清空 closedAt");
        assertNull(reopened.getSlaFulfilled(), "重开应清空 slaFulfilled");
        assertNotNull(reopened.getResponseBy(), "重开应重算 responseBy");

        // 取消场景:新工单 → 取消
        SupIssue i2 = basicIssue("TICKET-001B", sla);
        String rCancel = issueLifecycle.exec(List.of(i2), null,
            new String[]{SupIssueLifecycleHandler.CODE_CANCEL});
        assertTrue(rCancel.contains("成功 1"), "取消应成功,实际=" + rCancel);
        assertEquals(IssueStatus.CANCELLED.code,
            issueRepo.findById(i2.getId()).orElseThrow().getStatus());

        // 已关闭不可取消(已重开的工单再关闭后取消)
        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_REPLY});
        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_RESOLVE});
        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_CLOSE});
        String rReject = issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_CANCEL});
        assertTrue(rReject.contains("失败 1") && rReject.contains("不可取消"),
            "已关闭工单取消应被拒,实际=" + rReject);
    }

    // =================== (2) SLA 达成:在截止内响应+解决 → slaFulfilled=true ===================
    @Test
    void sup2_sla_fulfilled() {
        SupServiceLevelAgreement sla = seedSla(1440, 2880); // 24h/48h,宽松确保在截止内
        SupIssue i = basicIssue("TICKET-002", sla);
        slaEvaluator.applyDeadlines(i, i.getCreateTime());
        issueRepo.save(i);

        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_REPLY});
        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_RESOLVE});

        SupIssue after = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.RESOLVED.code, after.getStatus());
        assertEquals(Boolean.TRUE, after.getSlaFulfilled(), "在截止内完成应 SLA 达成");
        assertNotNull(after.getFirstResponseAt());
        assertNotNull(after.getResolvedAt());
        assertFalse(after.getFirstResponseAt().isAfter(after.getResponseBy()),
            "首次响应应在截止内,实际 firstResponseAt=" + after.getFirstResponseAt()
                + " responseBy=" + after.getResponseBy());
        assertFalse(after.getResolvedAt().isAfter(after.getResolutionBy()),
            "解决应在截止内,实际 resolvedAt=" + after.getResolvedAt()
                + " resolutionBy=" + after.getResolutionBy());
    }

    // =================== (3) SLA 违约:超过截止时间 → slaFulfilled=false ===================
    @Test
    void sup3_sla_breach() {
        SupServiceLevelAgreement sla = seedSla(60, 480);
        SupIssue i = basicIssue("TICKET-003", sla);
        slaEvaluator.applyDeadlines(i, i.getCreateTime());
        // 模拟已超时:把截止时间设为过去
        LocalDateTime past = LocalDateTime.now().minusMinutes(120);
        i.setResponseBy(past);
        i.setResolutionBy(past);
        issueRepo.save(i);

        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_REPLY});
        issueLifecycle.exec(List.of(i), null,
            new String[]{SupIssueLifecycleHandler.CODE_RESOLVE});

        SupIssue after = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(IssueStatus.RESOLVED.code, after.getStatus());
        assertEquals(Boolean.FALSE, after.getSlaFulfilled(), "超时应 SLA 违约");
        assertTrue(after.getFirstResponseAt().isAfter(after.getResponseBy()),
            "首次响应应超过截止,实际 firstResponseAt=" + after.getFirstResponseAt()
                + " responseBy=" + after.getResponseBy());
        assertTrue(after.getResolvedAt().isAfter(after.getResolutionBy()),
            "解决应超过截止,实际 resolvedAt=" + after.getResolvedAt()
                + " resolutionBy=" + after.getResolutionBy());
    }

    // =================== (4) 自动分派:按 SupSupportSettings 默认客服分派 + 历史 ===================
    @Test
    void sup4_auto_assign() {
        SupServiceLevelAgreement sla = seedSla(1440, 2880);
        SupIssue i = basicIssue("TICKET-004", sla);

        // settings 单例已由 Initializer 插入(autoAssignEnabled=false 默认)
        SupSupportSettings settings = settingsRepo.findFirstByOrderByIdAsc().orElseThrow();

        // 未开启场景:应返回提示
        settings.setAutoAssignEnabled(Boolean.FALSE);
        settings.setDefaultAgentId(null);
        settingsRepo.save(settings);
        String r1 = autoAssign.exec(i);
        assertTrue(r1.contains("未开启") || r1.contains("未配置"),
            "未开启应提示,实际=" + r1);

        // 开启 + 配默认客服
        settings.setAutoAssignEnabled(Boolean.TRUE);
        settings.setDefaultAgentId(9901L);
        settings.setDefaultAgentName("客服小张");
        settingsRepo.save(settings);

        String r2 = autoAssign.exec(i);
        assertTrue(r2.contains("已自动分派"), "开启后应分派成功,实际=" + r2);
        SupIssue after = issueRepo.findById(i.getId()).orElseThrow();
        assertEquals(9901L, after.getAssigneeId());
        assertEquals("客服小张", after.getAssigneeName());
        assertFalse(after.getAssignments().isEmpty(), "应有分派历史记录");
        SupIssueAssignment a = after.getAssignments().get(0);
        assertEquals(9901L, a.getAssigneeId());
        assertEquals("客服小张", a.getAssigneeName());
        assertNotNull(a.getAssignedAt());
    }

    // =================== (5) 知识库发布归档:草稿→发布→归档 ===================
    @Test
    void sup5_kb_publish() {
        SupKnowledgeBase art = new SupKnowledgeBase();
        art.setTitle("KB-001 系统使用指南");
        art.setCategory("操作手册");
        art.setContent("系统使用指南正文内容...");
        art.setStatus(KbStatus.DRAFT.code);
        art = kbRepo.save(art);

        // 发布:DRAFT → PUBLISHED + publishedAt
        String r1 = kbPublish.exec(List.of(art), null,
            new String[]{SupKbPublishHandler.CODE_PUBLISH});
        assertTrue(r1.contains("成功 1"), "发布应成功,实际=" + r1);
        SupKnowledgeBase published = kbRepo.findById(art.getId()).orElseThrow();
        assertEquals(KbStatus.PUBLISHED.code, published.getStatus());
        assertNotNull(published.getPublishedAt(), "发布应记 publishedAt");

        // 归档:PUBLISHED → ARCHIVED
        String r2 = kbPublish.exec(List.of(art), null,
            new String[]{SupKbPublishHandler.CODE_ARCHIVE});
        assertTrue(r2.contains("成功 1"), "归档应成功,实际=" + r2);
        assertEquals(KbStatus.ARCHIVED.code,
            kbRepo.findById(art.getId()).orElseThrow().getStatus());

        // 已归档再归档应失败(只有已发布状态可归档)
        String r3 = kbPublish.exec(List.of(art), null,
            new String[]{SupKbPublishHandler.CODE_ARCHIVE});
        assertTrue(r3.contains("失败 1") && r3.contains("只有已发布"),
            "已归档再归档应被拒,实际=" + r3);

        // 已归档可重新发布 → PUBLISHED
        String r4 = kbPublish.exec(List.of(art), null,
            new String[]{SupKbPublishHandler.CODE_PUBLISH});
        assertTrue(r4.contains("成功 1"), "归档重新发布应成功,实际=" + r4);
        assertEquals(KbStatus.PUBLISHED.code,
            kbRepo.findById(art.getId()).orElseThrow().getStatus());
    }

    // =================== (6) 客户关联:SupIssue.customer REF CrmCustomer(I-12) ===================
    @Test
    void sup6_customer_link() {
        // 先 save 客户(CrmCustomer 仅 name 必填,其他状态字段默认 0)
        CrmCustomer cust = new CrmCustomer();
        cust.setName("测试客户A");
        cust = customerRepo.save(cust);

        SupServiceLevelAgreement sla = seedSla(1440, 2880);
        SupIssue i = new SupIssue();
        i.setSubject("TICKET-006 客户工单");
        i.setPriority(IssuePriority.MEDIUM.code);
        i.setRaisedBy("客户A提交人");
        i.setSla(sla);
        i.setCustomer(cust);
        i.setStatus(IssueStatus.OPEN.code);
        i = issueRepo.save(i);

        SupIssue after = issueRepo.findById(i.getId()).orElseThrow();
        assertNotNull(after.getCustomer(), "客户 REF 应关联");
        assertEquals(cust.getId(), after.getCustomer().getId());
        assertEquals("测试客户A", after.getCustomer().getName(),
            "REF 取客户名应一致");

        // 同时验证 SLA 引擎可用客户专属 SLA(sla 直接指定路径)
        slaEvaluator.applyDeadlines(after, after.getCreateTime());
        assertNotNull(after.getResponseBy(), "应用 SLA 后应有 responseBy");
        assertNotNull(after.getResolutionBy(), "应用 SLA 后应有 resolutionBy");
    }
}
