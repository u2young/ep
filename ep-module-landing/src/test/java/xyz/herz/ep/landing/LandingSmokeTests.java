package xyz.herz.ep.landing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import xyz.herz.ep.landing.entity.LandingAccessLog;
import xyz.herz.ep.landing.entity.LandingLead;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.entity.LandingTemplate;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.LeadSource;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.TemplateCategory;
import xyz.herz.ep.landing.handler.LandingOfflineHandler;
import xyz.herz.ep.landing.handler.LandingPublishHandler;
import xyz.herz.ep.landing.jpa.LandingAccessLogRepository;
import xyz.herz.ep.landing.jpa.LandingLeadRepository;
import xyz.herz.ep.landing.jpa.LandingPageRepository;
import xyz.herz.ep.landing.jpa.LandingTemplateRepository;
import xyz.herz.ep.landing.shorturl.ShortCodeGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 落地页模块冒烟测试。
 * <p>覆盖:模板 CRUD + 落地页 CRUD + 状态机(发布/下线/重新发布)+
 * 短码生成(确定性/唯一性/可逆)+ 留资闭环 + UV 去重 + 状态机守护(禁止直改 status)。
 *
 * <p>独立 H2 内存库,不依赖 ep-boot 层,magic-api 接口不在本测试范围。
 */
@SpringBootTest(classes = LandingTestApplication.class)
@Transactional
class LandingSmokeTests {

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private LandingTemplateRepository tplRepo;
    @Autowired private LandingLeadRepository leadRepo;
    @Autowired private LandingAccessLogRepository accessRepo;
    @Autowired private LandingPublishHandler publishHandler;
    @Autowired private LandingOfflineHandler offlineHandler;
    @Autowired private ShortCodeGenerator codeGen;

    // ============ 1. 模板 CRUD ============
    @Test
    void template_crud() {
        LandingTemplate t = new LandingTemplate();
        t.setName("留资表单模板");
        t.setCategory(TemplateCategory.FORM_LEAD.code);
        t.setSchema("{\"type\":\"page\",\"body\":[{\"type\":\"form\"}]}");
        t.setEnabled(EnableStatus.ENABLED.code);
        tplRepo.save(t);
        assertNotNull(t.getId());

        LandingTemplate db = tplRepo.findById(t.getId()).orElseThrow();
        assertEquals("留资表单模板", db.getName());
        assertEquals(TemplateCategory.FORM_LEAD.code, db.getCategory());

        tplRepo.delete(db);
        assertTrue(tplRepo.findById(t.getId()).isEmpty());
    }

    // ============ 2. 落地页草稿保存 ============
    @Test
    void page_draft_save_and_load() {
        LandingPage p = newPage("hello", "测试页", "{\"type\":\"page\"}");
        pageRepo.save(p);
        LandingPage db = pageRepo.findById(p.getId()).orElseThrow();
        assertEquals(PageStatus.DRAFT.code, db.getStatus());
        assertEquals("{\"type\":\"page\"}", db.getContent());
        assertEquals(0L, db.getPvCount());
        assertNull(db.getShortCode());
    }

    // ============ 3. 状态机:草稿 → 发布 → 下线 → 重新发布 ============
    @Test
    void page_state_machine_publish_offline_republish() {
        LandingPage p = newPage("psm", "状态机测试", "{}");
        pageRepo.saveAndFlush(p);

        // 草稿 → 发布
        publishHandler.exec(List.of(p), null, new String[]{LandingPublishHandler.CODE});
        LandingPage pub = pageRepo.findById(p.getId()).orElseThrow();
        assertEquals(PageStatus.PUBLISHED.code, pub.getStatus());
        assertNotNull(pub.getShortCode());
        assertEquals(6, pub.getShortCode().length());
        assertNotNull(pub.getPublishTime());
        assertNull(pub.getOfflineTime());
        String firstCode = pub.getShortCode();

        // 发布 → 下线
        offlineHandler.exec(List.of(pub), null, new String[]{LandingOfflineHandler.CODE});
        LandingPage off = pageRepo.findById(p.getId()).orElseThrow();
        assertEquals(PageStatus.OFFLINE.code, off.getStatus());
        assertNotNull(off.getOfflineTime());
        // 短码保留
        assertEquals(firstCode, off.getShortCode());

        // 下线 → 重新发布(复用原短码)
        publishHandler.exec(List.of(off), null, new String[]{LandingPublishHandler.CODE});
        LandingPage repub = pageRepo.findById(p.getId()).orElseThrow();
        assertEquals(PageStatus.PUBLISHED.code, repub.getStatus());
        assertEquals(firstCode, repub.getShortCode()); // 复用,不重新生成
        assertNotNull(repub.getPublishTime());
        assertNull(repub.getOfflineTime());
    }

    // ============ 4. 短码生成器:确定性 + 唯一性 + 可逆 ============
    @Test
    void short_code_deterministic_unique_reversible() {
        // 确定性:同 id 必同码
        assertEquals(codeGen.generate(1L), codeGen.generate(1L));
        // 唯一性:不同 id 必异码
        assertNotEquals(codeGen.generate(1L), codeGen.generate(2L));
        // 长度固定 6 位
        assertEquals(6, codeGen.generate(1L).length());
        assertEquals(6, codeGen.generate(1_000_000L).length());
        assertEquals(6, codeGen.generate(999_999_999L).length());
        // 可逆:decode 回原 id
        String code = codeGen.generate(12345L);
        assertEquals(12345L, codeGen.decode(code));
    }

    // ============ 5. 留资闭环 ============
    @Test
    void lead_submit_persisted() {
        LandingPage p = newPage("lead", "留资测试", "{}");
        pageRepo.saveAndFlush(p);
        publishHandler.exec(List.of(p), null, new String[]{LandingPublishHandler.CODE});
        LandingPage pub = pageRepo.findById(p.getId()).orElseThrow();

        // 模拟 magic-api /landing/lead 接口写入
        LandingLead lead = new LandingLead();
        lead.setPageId(pub.getId());
        lead.setSlug(pub.getSlug());
        lead.setPhone("13800138000");
        lead.setExtra("{\"name\":\"张三\"}");
        lead.setSource(LeadSource.SHORT_URL.code);
        lead.setClientIp("127.0.0.1");
        lead.setUserAgent("test");
        lead.setSubmitTime(LocalDateTime.now());
        leadRepo.saveAndFlush(lead);

        List<LandingLead> leads = leadRepo.findByPageId(pub.getId());
        assertEquals(1, leads.size());
        assertEquals("13800138000", leads.get(0).getPhone());
        assertEquals("{\"name\":\"张三\"}", leads.get(0).getExtra());
    }

    // ============ 6. UV 去重(同 IP+指纹 唯一约束) ============
    @Test
    void uv_dedup() {
        LandingPage p = newPage("uv", "UV测试", "{}");
        pageRepo.saveAndFlush(p);

        LandingAccessLog log1 = makeAccessLog(p, "1.1.1.1", "fp001");
        accessRepo.saveAndFlush(log1);

        // 同 pageId+IP+指纹 第二次:唯一约束冲突
        // 注:异常后 Hibernate session 被标记 rollback-only,本方法内不再做后续 DB 操作
        assertThrows(DataIntegrityViolationException.class, () ->
            accessRepo.saveAndFlush(makeAccessLog(p, "1.1.1.1", "fp001")));
    }

    // ============ 7. 状态机守护:DataProxy 禁止非草稿态直改 status ============
    @Test
    void state_proxy_blocks_direct_status_edit() {
        LandingPage.Proxy proxy = new LandingPage.Proxy();
        LandingPage p = newPage("bad", "尝试直改", "{}");

        // 草稿态(0/null)允许直接编辑
        p.setStatus(PageStatus.DRAFT.code);
        assertDoesNotThrow(() -> proxy.beforeUpdate(p));
        p.setStatus(null);
        assertDoesNotThrow(() -> proxy.beforeUpdate(p));

        // 已发布/已下线态禁止直接编辑
        p.setStatus(PageStatus.PUBLISHED.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(p));
        p.setStatus(PageStatus.OFFLINE.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(p));
    }

    // ============ 8. 非草稿态发布被拒绝 ============
    @Test
    void publish_rejected_when_not_draft_or_offline() {
        LandingPage p = newPage("rej", "重复发布", "{}");
        pageRepo.saveAndFlush(p);
        publishHandler.exec(List.of(p), null, new String[]{LandingPublishHandler.CODE});
        // 已发布态再发布:失败
        String result = publishHandler.exec(List.of(p), null, new String[]{LandingPublishHandler.CODE});
        assertTrue(result.contains("失败 1"), "已发布态不应再发布: " + result);
    }

    // ============ TR-3.1/3.2 @DragSort 拖拽排序(RED→GREEN) ============
    @Test
    void landing_template_drag_sort_annotation_and_order() throws Exception {
        // TR-3.2 注解: @Erupt.dragSort.field 必须 = "sort"
        xyz.erupt.annotation.Erupt eruptAnn =
            LandingTemplate.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertNotNull(eruptAnn, "LandingTemplate 应有 @Erupt");
        assertEquals("sort", eruptAnn.dragSort().field(),
            "LandingTemplate 必须配置 @DragSort(field=\"sort\") 支持列表拖拽排序");

        // TR-3.2 字段完整性: sort 必须存在,Integer 类型,默认值 0(用反射避免 RED 编译失败)
        java.lang.reflect.Field sortF = LandingTemplate.class.getDeclaredField("sort");
        sortF.setAccessible(true);
        assertEquals(Integer.class, sortF.getType(), "LandingTemplate.sort 类型必须是 Integer");
        LandingTemplate empty = new LandingTemplate();
        assertEquals(0, sortF.get(empty), "LandingTemplate.sort 默认值应为 0");

        java.lang.reflect.Method setSort = LandingTemplate.class.getMethod("setSort", Integer.class);
        java.lang.reflect.Method getSort = LandingTemplate.class.getMethod("getSort");

        // TR-3.1 排序查询: 存 3 条(sort=3/1/2),按 sort ASC 顺序应为 1,2,3
        LandingTemplate t3 = new LandingTemplate();
        t3.setName("t-sort-3"); t3.setCategory(TemplateCategory.BLANK.code);
        setSort.invoke(t3, 3); t3.setEnabled(EnableStatus.ENABLED.code);
        LandingTemplate t1 = new LandingTemplate();
        t1.setName("t-sort-1"); t1.setCategory(TemplateCategory.BLANK.code);
        setSort.invoke(t1, 1); t1.setEnabled(EnableStatus.ENABLED.code);
        LandingTemplate t2 = new LandingTemplate();
        t2.setName("t-sort-2"); t2.setCategory(TemplateCategory.BLANK.code);
        setSort.invoke(t2, 2); t2.setEnabled(EnableStatus.ENABLED.code);
        tplRepo.saveAll(List.of(t3, t1, t2));
        // 只取本次新插入的 3 条(前缀 t-sort-),避免受 Landing 模块预置初始化模板(如空白页/海报页)干扰
        List<LandingTemplate> ordered = tplRepo.findAll().stream()
            .filter(t -> t.getName() != null && t.getName().startsWith("t-sort-"))
            .sorted((a, b) -> Integer.compare(
                (Integer) unchecked(getSort, a), (Integer) unchecked(getSort, b)))
            .toList();
        assertEquals(3, ordered.size(), "新建的 t-sort-* 模板应恰好 3 条");
        assertEquals("t-sort-1", ordered.get(0).getName());
        assertEquals("t-sort-2", ordered.get(1).getName());
        assertEquals("t-sort-3", ordered.get(2).getName());
    }

    /** 反射工具: 忽略受检异常,便于 RED 阶段的流式 lambda 调用。 */
    private static Object unchecked(java.lang.reflect.Method m, Object target) {
        try { return m.invoke(target); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    // ============ helpers ============
    private LandingPage newPage(String slug, String name, String content) {
        LandingPage p = new LandingPage();
        p.setSlug(slug);
        p.setName(name);
        p.setContent(content);
        return p;
    }

    private LandingAccessLog makeAccessLog(LandingPage p, String ip, String fp) {
        LandingAccessLog log = new LandingAccessLog();
        log.setPageId(p.getId());
        log.setSlug(p.getSlug());
        log.setClientIp(ip);
        log.setClientFp(fp);
        log.setAccessTime(LocalDateTime.now());
        return log;
    }
}
