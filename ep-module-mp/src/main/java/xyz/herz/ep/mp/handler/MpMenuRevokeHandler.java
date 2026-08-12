package xyz.herz.ep.mp.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mp.entity.MpMenu;
import xyz.herz.ep.mp.enums.MpDictEnums.MenuStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 菜单「撤回」按钮处理器:status 1(已发布)→ 0(草稿)。
 * <p>通过 {@code @RowOperation(operationHandler = MpMenuRevokeHandler.class)} 绑定。
 */
@Component
public class MpMenuRevokeHandler implements OperationHandler<Object, Object> {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, skip = 0;
        for (Object row : data) {
            if (!(row instanceof MpMenu menu)) continue;
            if (MenuStatus.DRAFT.code == menu.getStatus()) { skip++; continue; }
            menu.setStatus(MenuStatus.DRAFT.code);
            em.merge(menu);
            ok++;
        }
        return String.format("撤回成功 %d, 跳过 %d", ok, skip);
    }
}
