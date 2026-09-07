package xyz.herz.ep.mfg.handler.master;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.enums.MfgDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 制造模块主数据通用「启用/停用」行按钮处理器(参考 ERP 模板 ErpMasterToggleHandler)。
 * <p>适用于:MfgWorkstation / MfgOperation / MfgBom(都有 status + setStatus)。
 * <p>状态迁移:DISABLED(0) ↔ ENABLED(1)。
 */
@Component
public class MfgMasterToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE = "mfg.master.enable";
    public static final String DISABLE = "mfg.master.disable";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        boolean toEnable = ENABLE.equals(code);
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                Integer target = toEnable ? EnableStatus.ENABLED.code : EnableStatus.DISABLED.code;
                Integer cur = (Integer) row.getClass().getMethod("getStatus").invoke(row);
                if (cur != null && cur.equals(target)) {
                    sb.append(row.getClass().getSimpleName()).append(" 已是目标状态; ");
                    continue;
                }
                setStatus(row, target);
                em.merge(row);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void setStatus(Object bean, int v) throws ReflectiveOperationException {
        try {
            java.lang.reflect.Method s = bean.getClass().getMethod("setStatus", Integer.class);
            s.invoke(bean, v);
        } catch (NoSuchMethodException e1) {
            java.lang.reflect.Method s = bean.getClass().getMethod("setStatus", int.class);
            s.invoke(bean, v);
        }
    }
}
