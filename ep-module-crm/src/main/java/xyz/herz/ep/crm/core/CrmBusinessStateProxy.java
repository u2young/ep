package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.entity.CrmBusiness;

import java.math.BigDecimal;

/**
 * 商机状态机 DataProxy:
 *   - endStatus 禁止表单直接编辑
 *   - totalPrice 在保存前根据折扣自动重算
 */
public class CrmBusinessStateProxy extends CrmStateDataProxy<CrmBusiness> {
    @Override
    protected String stateFieldName() { return "endStatus"; }

    @Override
    public void beforeUpdate(CrmBusiness target) {
        // endStatus 禁止表单直改
        if (target.getEndStatus() != null) {
            throw new IllegalArgumentException("结束状态 endStatus 禁止表单直接修改,请使用行按钮(赢单/输单/无效)变更。");
        }
        // 保存前重算 totalPrice
        recalcTotal(target);
    }

    @Override
    public void beforeAdd(CrmBusiness target) {
        recalcTotal(target);
    }

    public static void recalcTotal(CrmBusiness b) {
        Long prod = b.getTotalProductPrice() == null ? 0L : b.getTotalProductPrice();
        BigDecimal disc = b.getDiscountPercent();
        long price = prod;
        if (disc != null && disc.compareTo(BigDecimal.valueOf(100)) != 0) {
            price = BigDecimal.valueOf(prod)
                .multiply(disc)
                .divide(BigDecimal.valueOf(100), 0, BigDecimal.ROUND_HALF_UP)
                .longValue();
        }
        b.setTotalPrice(price);
    }
}
