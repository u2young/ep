package xyz.herz.ep.erp.handler.master;

import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.ProductListingStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * ERP 主数据通用行按钮处理器(启停/上架下架/设默认仓)。
 * <p>通过 {@code @RowOperation(operationHandler = ErpMasterToggleHandler.class,
 *   operationParam = { ErpMasterToggleHandler.ENABLE })} 等绑定。
 */
@Component
public class ErpMasterToggleHandler implements OperationHandler<Object, Object> {

    public static final String ENABLE   = "erp.enable";
    public static final String DISABLE  = "erp.disable";
    public static final String LIST     = "erp.list";
    public static final String DELIST   = "erp.delist";
    public static final String DEFAULT_WH = "erp.wh.default";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : ENABLE;
        int ok = 0, skip = 0;
        for (Object row : data) {
            if (row instanceof ErpProduct product) {
                if (applyStatusListing(product, code)) ok++; else skip++;
                em.merge(product);
            } else if (row instanceof ErpWarehouse wh) {
                if (applyWarehouse(wh, code)) ok++; else skip++;
                if (DEFAULT_WH.equals(code)) {
                    em.createQuery("UPDATE ErpWarehouse w SET w.defaultFlag = false" +
                                   " WHERE w.defaultFlag = true AND w.id <> :id")
                      .setParameter("id", wh.getId()).executeUpdate();
                }
                em.merge(wh);
            } else {
                // 其他主数据:只有启停,统一反射
                try {
                    if (applyGenericStatus(row, code)) ok++; else skip++;
                    em.merge(row);
                } catch (Exception e) {
                    throw new IllegalStateException("启停按钮操作失败: " + row.getClass().getSimpleName(), e);
                }
            }
        }
        return String.format("成功 %d, 跳过 %d (code=%s)", ok, skip, code);
    }

    // ================= 具体分派 =================

    private boolean applyStatusListing(ErpProduct p, String code) {
        if (LIST.equals(code)) {
            if (EnableStatus.DISABLED.code == p.getStatus()) throw new IllegalStateException("请先启用产品再上架");
            p.setListingStatus(ProductListingStatus.LISTED.code);
            return true;
        }
        if (DELIST.equals(code)) {
            p.setListingStatus(ProductListingStatus.DELISTED.code);
            return true;
        }
        if (ENABLE.equals(code)) {
            p.setStatus(EnableStatus.ENABLED.code);
            return true;
        }
        if (DISABLE.equals(code)) {
            p.setStatus(EnableStatus.DISABLED.code);
            p.setListingStatus(ProductListingStatus.DELISTED.code);
            return true;
        }
        return false;
    }

    private boolean applyWarehouse(ErpWarehouse w, String code) {
        if (ENABLE.equals(code)) {
            w.setStatus(EnableStatus.ENABLED.code);
            return true;
        }
        if (DISABLE.equals(code)) {
            w.setStatus(EnableStatus.DISABLED.code);
            return true;
        }
        if (DEFAULT_WH.equals(code)) {
            w.setDefaultFlag(true);
            return true;
        }
        return false;
    }

    // 品牌 / 单位 / 账户 / 供应商 / 客户:都有 status(Integer) + setStatus
    private boolean applyGenericStatus(Object bean, String code)
            throws ReflectiveOperationException {
        if (ENABLE.equals(code)) return setStatus(bean, EnableStatus.ENABLED.code);
        if (DISABLE.equals(code)) return setStatus(bean, EnableStatus.DISABLED.code);
        return false;
    }

    private boolean setStatus(Object bean, int v) throws ReflectiveOperationException {
        try {
            java.lang.reflect.Method s = bean.getClass().getMethod("setStatus", Integer.class);
            s.invoke(bean, v);
            return true;
        } catch (NoSuchMethodException e1) {
            java.lang.reflect.Method s = bean.getClass().getMethod("setStatus", int.class);
            s.invoke(bean, v);
            return true;
        }
    }
}
