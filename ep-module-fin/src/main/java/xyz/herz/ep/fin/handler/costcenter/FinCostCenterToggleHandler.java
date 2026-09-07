package xyz.herz.ep.fin.handler.costcenter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 成本中心「启用/停用」行按钮处理器。
 * <p>状态迁移:DISABLED(0) ↔ ENABLED(1)。
 */
@Component
public class FinCostCenterToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE = "fin.cost_center.enable";
    public static final String DISABLE = "fin.cost_center.disable";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        boolean toEnable = ENABLE.equals(code);
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof FinCostCenter doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持成本中心; ");
                continue;
            }
            try {
                Integer target = toEnable ? EnableStatus.ENABLED.code : EnableStatus.DISABLED.code;
                if (doc.getStatus() != null && doc.getStatus().equals(target)) {
                    sb.append("成本中心 ").append(doc.getCode()).append(" 已是目标状态; ");
                    continue;
                }
                doc.setStatus(target);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("成本中心#").append(doc.getCode()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
