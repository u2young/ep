package xyz.herz.ep.landing.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.jpa.LandingPageRepository;
import xyz.herz.ep.landing.shorturl.ShortCodeGenerator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 落地页「发布」行按钮处理器。
 * <p>草稿(0) / 已下线(20) → 已发布(10):
 * <ul>
 *   <li>生成 6 位短码(Base62(id + 1_000_000),仅首次发布生成,下线后重新发布复用原短码)</li>
 *   <li>设置 publishTime,清空 offlineTime</li>
 * </ul>
 */
@Component
public class LandingPublishHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "landing.publish";

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private ShortCodeGenerator codeGen;

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
                if (st != null && st != PageStatus.DRAFT.code && st != PageStatus.OFFLINE.code) {
                    throw new IllegalStateException("当前状态不允许发布: " + st);
                }
                // 首次发布生成短码;下线后重新发布复用原短码
                if (p.getShortCode() == null || p.getShortCode().isBlank()) {
                    p.setShortCode(codeGen.generate(p.getId()));
                }
                p.setStatus(PageStatus.PUBLISHED.code);
                p.setPublishTime(LocalDateTime.now());
                p.setOfflineTime(null);
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
