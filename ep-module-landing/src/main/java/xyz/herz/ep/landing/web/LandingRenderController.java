package xyz.herz.ep.landing.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.jpa.LandingPageRepository;
import xyz.herz.ep.landing.shorturl.ShortUrlController;

import java.util.Optional;

/**
 * H5 落地页渲染 Controller:/p/{slug}。
 * <p>普通 Spring MVC Controller,EruptSecurityInterceptor 不拦截(只拦 @EruptRouter),
 * 因此 C 端用户免登访问。
 * <p>从 DB 取已发布页面的 amis JSON schema,通过 Thymeleaf 注入到 render.html 模板,
 * 前端用 amis SDK 的 embed 函数渲染。
 * <p>访问统计复用 {@link ShortUrlController} 的 recordAccess 逻辑(通过短链进入会先到 /l/{code})。
 */
@Controller
public class LandingRenderController {

    @Autowired private LandingPageRepository pageRepo;

    @GetMapping("/p/{slug}")
    public String render(@PathVariable String slug, Model model, HttpServletRequest req) {
        Optional<LandingPage> opt = pageRepo.findBySlug(slug);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "页面不存在");
        }
        LandingPage p = opt.get();
        if (p.getStatus() == null || p.getStatus() != PageStatus.PUBLISHED.code) {
            throw new ResponseStatusException(HttpStatus.GONE, "页面已下线");
        }

        // amis schema 由 Thymeleaf 后端注入,避免前端额外 fetch
        String schema = p.getContent() == null || p.getContent().isBlank()
            ? "{\"type\":\"page\",\"body\":[{\"type\":\"tpl\",\"tpl\":\"页面内容为空\"}]}"
            : p.getContent();
        model.addAttribute("pageTitle", p.getName() == null ? "H5 落地页" : p.getName());
        model.addAttribute("schema", schema);
        model.addAttribute("slug", slug);
        model.addAttribute("pageId", p.getId());
        return "landing/render";
    }
}
