package xyz.herz.ep.fin.handler.account;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 会计科目「启用/停用」行按钮处理器(参考 ERP 模板 ErpMasterToggleHandler)。
 * <p>状态迁移:DISABLED(0) ↔ ENABLED(1)。
 */
@Component
public class FinAccountToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE = "fin.account.enable";
    public static final String DISABLE = "fin.account.disable";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        boolean toEnable = ENABLE.equals(code);
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinAccount doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持会计科目; ");
                continue;
            }
            try {
                Integer target = toEnable ? EnableStatus.ENABLED.code : EnableStatus.DISABLED.code;
                if (doc.getStatus() != null && doc.getStatus().equals(target)) {
                    sb.append("科目 ").append(doc.getCode()).append(" 已是目标状态; ");
                    continue;
                }
                doc.setStatus(target);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("科目#").append(doc.getCode()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
