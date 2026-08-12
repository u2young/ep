package xyz.herz.ep.iot.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.iot.entity.IotProduct;
import xyz.herz.ep.iot.enums.IotDictEnums.EnableStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 产品启停行按钮处理器(参考 ERP 的 ErpMasterToggleHandler)。
 * <p>通过 {@code @RowOperation(operationHandler = IotProductToggleHandler.class,
 *   operationParam = { IotProductToggleHandler.ENABLE })} 绑定。
 */
@Component
public class IotProductToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE  = "iot.product.enable";
    public static final String DISABLE = "iot.product.disable";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        int ok = 0;
        for (Object row : data) {
            if (row instanceof IotProduct p) {
                p.setStatus(ENABLE.equals(code)
                    ? EnableStatus.ENABLED.code
                    : EnableStatus.DISABLED.code);
                em.merge(p);
                ok++;
            }
        }
        return String.format("成功 %d (code=%s)", ok, code);
    }
}
