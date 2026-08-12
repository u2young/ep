package xyz.herz.ep.mp.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mp.entity.MpAutoReply;
import xyz.herz.ep.mp.enums.MpDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 自动回复「启用/禁用」切换处理器。
 * <p>通过 {@code @RowOperation(operationHandler = MpReplyToggleHandler.class,
 *   operationParam = { "ENABLE" })} 绑定,eruptParams[0] 取 ENABLE / DISABLE。
 */
@Component
public class MpReplyToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE = "ENABLE";
    public static final String DISABLE = "DISABLE";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        int target = ENABLE.equals(code) ? EnableStatus.ENABLED.code : EnableStatus.DISABLED.code;
        int ok = 0, skip = 0;
        for (Object row : data) {
            if (!(row instanceof MpAutoReply reply)) continue;
            if (reply.getStatus() != null && reply.getStatus() == target) { skip++; continue; }
            reply.setStatus(target);
            em.merge(reply);
            ok++;
        }
        return String.format("%s 成功 %d, 跳过 %d", code, ok, skip);
    }
}
