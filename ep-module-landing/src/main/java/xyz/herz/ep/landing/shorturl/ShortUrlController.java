package xyz.herz.ep.landing.shorturl;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import xyz.herz.ep.landing.entity.LandingAccessLog;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.jpa.LandingAccessLogRepository;
import xyz.herz.ep.landing.jpa.LandingPageRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 短链接 Controller:/l/{code} → 重定向到 /p/{slug}。
 * <p>普通 Spring MVC Controller,EruptSecurityInterceptor 不拦截(只拦 @EruptRouter),
 * 因此 C 端用户免登访问。
 * <p>PV 每次访问 +1;UV 同 IP+指纹当日首次 +1(依赖 landing_access_log 唯一约束)。
 */
@Controller
public class ShortUrlController {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlController.class);

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private LandingAccessLogRepository accessRepo;

    @GetMapping("/l/{code}")
    public String redirect(@PathVariable String code, HttpServletRequest req) {
        Optional<LandingPage> opt = pageRepo.findByShortCode(code);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "短码无效");
        }
        LandingPage p = opt.get();
        if (p.getStatus() == null || p.getStatus() != PageStatus.PUBLISHED.code) {
            throw new ResponseStatusException(HttpStatus.GONE, "页面已下线");
        }
        recordAccess(p, req);
        return "redirect:/p/" + p.getSlug();
    }

    /** 记录访问日志(PV 每次+1,UV 首次+1)。异常仅记日志不影响重定向。 */
    private void recordAccess(LandingPage p, HttpServletRequest req) {
        try {
            String ip = clientIp(req);
            String ua = req.getHeader("User-Agent") == null ? "" : req.getHeader("User-Agent");
            String fp = md5Short(ip + ua);

            LandingAccessLog logEntry = new LandingAccessLog();
            logEntry.setPageId(p.getId());
            logEntry.setSlug(p.getSlug());
            logEntry.setClientIp(ip);
            logEntry.setClientFp(fp);
            logEntry.setAccessTime(LocalDateTime.now());
            try {
                accessRepo.saveAndFlush(logEntry);
                // UV +1(首次插入成功)
                pageRepo.incrementUv(p.getId());
            } catch (Exception dup) {
                // 唯一约束冲突 = 已访问过,UV 不增
            }
            // PV 每次 +1
            pageRepo.incrementPv(p.getId());
        } catch (Exception ex) {
            log.warn("recordAccess 失败 pageId={},err={}", p.getId(), ex.getMessage());
        }
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }

    /** MD5 取前 32 位,用于 UV 指纹去重。 */
    private static String md5Short(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
