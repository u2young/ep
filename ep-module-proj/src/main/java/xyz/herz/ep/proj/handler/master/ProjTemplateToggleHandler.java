package xyz.herz.ep.proj.handler.master;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.proj.entity.template.ProjProjectTemplate;
import xyz.herz.ep.proj.enums.ProjDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 项目模板启停切换行按钮处理器(启用/停用,二合一)。
 * <p>状态切换:ENABLED(1) ⇄ DISABLED(0)。
 */
@Component
public class ProjTemplateToggleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_ENABLE = "proj.template.enable";
    public static final String CODE_DISABLE = "proj.template.disable";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_ENABLE;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ProjProjectTemplate doc)) {
                fail++; sb.append("仅支持项目模板; "); continue;
            }
            try {
                Integer st = doc.getStatus();
                if (st == null) st = EnableStatus.DISABLED.code;
                switch (code) {
                    case CODE_ENABLE -> {
                        if (st == EnableStatus.ENABLED.code) {
                            sb.append("模板 ").append(doc.getCode()).append(" 已启用; "); continue;
                        }
                        doc.setStatus(EnableStatus.ENABLED.code);
                    }
                    case CODE_DISABLE -> {
                        if (st == EnableStatus.DISABLED.code) {
                            sb.append("模板 ").append(doc.getCode()).append(" 已停用; "); continue;
                        }
                        doc.setStatus(EnableStatus.DISABLED.code);
                    }
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("模板#").append(doc.getCode()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
