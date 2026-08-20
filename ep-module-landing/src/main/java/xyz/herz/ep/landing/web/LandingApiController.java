package xyz.herz.ep.landing.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.herz.ep.landing.entity.LandingLead;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.entity.LandingTemplate;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.LeadSource;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.jpa.LandingLeadRepository;
import xyz.herz.ep.landing.jpa.LandingPageRepository;
import xyz.herz.ep.landing.jpa.LandingTemplateRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 落地页公开 API Controller(magic-api 兜底实现)。
 * <p>magic-api 是首选实现(脚本见 src/main/resources/magic-api/api/landing/),
 * 当 magic-api 未加载或脚本未导入时,本 Controller 提供等效功能,确保开箱即用。
 * <p>统一返回格式:{code, msg, data}(与 magic-api 配置的 response 模板一致)。
 *
 * <p>公开接口(免登):
 * <ul>
 *   <li>GET  /api/landing/page/slug/{slug} — H5 渲染取已发布页面</li>
 *   <li>GET  /api/landing/template/list    — 模板列表</li>
 *   <li>POST /api/landing/lead             — 留资提交</li>
 * </ul>
 *
 * <p>普通 Spring MVC,EruptSecurityInterceptor 不拦截(只拦 @EruptRouter),C 端免登。
 */
@RestController
@RequestMapping("/api/landing")
public class LandingApiController {

    private static final Logger log = LoggerFactory.getLogger(LandingApiController.class);

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private LandingTemplateRepository tplRepo;
    @Autowired private LandingLeadRepository leadRepo;

    // ============ 公开接口(免登) ============

    /** 按 slug 取已发布页面(H5 渲染用)。 */
    @GetMapping("/page/slug/{slug}")
    public ResponseEntity<Map<String, Object>> getBySlug(@PathVariable String slug) {
        Optional<LandingPage> opt = pageRepo.findBySlug(slug);
        if (opt.isEmpty()) {
            return ok(404, "页面不存在或已下线", null);
        }
        LandingPage p = opt.get();
        if (p.getStatus() == null || p.getStatus() != PageStatus.PUBLISHED.code) {
            return ok(404, "页面不存在或已下线", null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("slug", p.getSlug());
        data.put("name", p.getName());
        data.put("content", p.getContent());
        data.put("status", p.getStatus());
        return ok(1, "ok", data);
    }

    /** 模板列表(已启用)。 */
    @GetMapping("/template/list")
    public ResponseEntity<Map<String, Object>> templateList() {
        List<LandingTemplate> all = tplRepo.findAll().stream()
            .filter(t -> t.getEnabled() != null && t.getEnabled() == EnableStatus.ENABLED.code)
            .toList();
        List<Map<String, Object>> data = all.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("category", t.getCategory());
            m.put("schema", t.getSchema());
            m.put("thumbUrl", t.getThumbUrl());
            return m;
        }).collect(Collectors.toList());
        return ok(1, "ok", data);
    }

    /** 留资提交(核心闭环)。 */
    @PostMapping("/lead")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitLead(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest req) {
        Long pageId = toLong(body.get("pageId"));
        if (pageId == null) {
            return ok(400, "pageId 必填", null);
        }
        Optional<LandingPage> opt = pageRepo.findById(pageId);
        if (opt.isEmpty() || opt.get().getStatus() != PageStatus.PUBLISHED.code) {
            return ok(404, "页面不存在或已下线", null);
        }
        LandingPage p = opt.get();

        LandingLead lead = new LandingLead();
        lead.setPageId(p.getId());
        lead.setSlug(p.getSlug());
        lead.setPhone(toStr(body.get("phone")));
        Object extra = body.get("extra");
        lead.setExtra(extra == null ? null : extra.toString());
        Integer source = toInt(body.get("source"));
        lead.setSource(source == null ? LeadSource.SHORT_URL.code : source);
        lead.setClientIp(clientIp(req));
        String ua = req.getHeader("User-Agent");
        lead.setUserAgent(ua == null ? "" : ua);
        lead.setSubmitTime(LocalDateTime.now());
        leadRepo.save(lead);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("leadId", lead.getId());
        return ok(1, "提交成功", data);
    }

    // ============ 管理接口(需登录,这里用 Erupt 后台替代,magic-api 脚本另有) ============

    /** 按 ID 取页面(管理端)。 */
    @GetMapping("/page/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Optional<LandingPage> opt = pageRepo.findById(id);
        if (opt.isEmpty()) {
            return ok(404, "页面不存在", null);
        }
        return ok(1, "ok", opt.get());
    }

    // ============ helpers ============

    private static ResponseEntity<Map<String, Object>> ok(int code, String msg, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", code);
        r.put("msg", msg);
        r.put("data", data);
        return ResponseEntity.ok(r);
    }

    private static String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }
}
