package xyz.herz.ep.landing.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.jpa.LandingPageRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 落地页「下线」行按钮处理器。
 * <p>已发布(10) → 已下线(20):
 * <ul>
 *   <li>设置 offlineTime</li>
 *   <li>短码保留(便于排查历史留资),但访问 /l/{code} 返回 410 Gone</li>
 *   <li>直链 /p/{slug} 也返回 410 Gone</li>
 * </ul>
 */
@Component
public class LandingOfflineHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "landing.offline";

    @Autowired private LandingPageRepository pageRepo;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof LandingPage p)) {
                    throw new IllegalArgumentException("仅支持落地页");
                }
                Integer st = p.getStatus();
                if (st == null || st != PageStatus.PUBLISHED.code) {
                    throw new IllegalStateException("仅已发布状态可下线,当前: " + st);
                }
                p.setStatus(PageStatus.OFFLINE.code);
                p.setOfflineTime(LocalDateTime.now());
                pageRepo.save(p);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("err: ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
