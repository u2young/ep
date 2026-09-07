package xyz.herz.ep.sup.handler.kb;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.sup.entity.kb.SupKnowledgeBase;
import xyz.herz.ep.sup.enums.SupDictEnums.KbStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库发布/归档行按钮处理器(二合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>PUBLISH:DRAFT(0)/ARCHIVED(2) → PUBLISHED(1),回写 publishedAt。</li>
 *   <li>ARCHIVE:PUBLISHED(1) → ARCHIVED(2)。</li>
 * </ul>
 */
@Component
public class SupKbPublishHandler implements OperationHandler<Object, Object> {

    public static final String CODE_PUBLISH = "sup.kb.publish";
    public static final String CODE_ARCHIVE = "sup.kb.archive";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_PUBLISH;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof SupKnowledgeBase doc)) {
                fail++; sb.append("仅支持知识库文章; "); continue;
            }
            try {
                switch (code) {
                    case CODE_PUBLISH -> applyPublish(doc);
                    case CODE_ARCHIVE -> applyArchive(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("文章#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyPublish(SupKnowledgeBase doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != KbStatus.DRAFT.code && st != KbStatus.ARCHIVED.code)) {
            throw new IllegalStateException("只有草稿/已归档状态可发布,当前状态=" + st);
        }
        doc.setStatus(KbStatus.PUBLISHED.code);
        doc.setPublishedAt(LocalDateTime.now());
    }

    private void applyArchive(SupKnowledgeBase doc) {
        Integer st = doc.getStatus();
        if (st == null || st != KbStatus.PUBLISHED.code) {
            throw new IllegalStateException("只有已发布状态可归档,当前状态=" + st);
        }
        doc.setStatus(KbStatus.ARCHIVED.code);
    }
}
