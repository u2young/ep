package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.entity.CrmCustomer;

/** 客户状态机 DataProxy:锁定 lockStatus / dealStatus / followUpStatus 三字段,禁止表单直改。 */
public class CrmCustomerStateProxy extends CrmStateDataProxy<CrmCustomer> {
    @Override
    protected String stateFieldName() { return "lockStatus"; }

    @Override
    public void beforeUpdate(CrmCustomer target) {
        // 三字段全部:值非 null 非默认(0)就报错,用户一定是在表单里改了的
        checkField(target, "lockStatus", 0, 1);
        checkField(target, "dealStatus", 0, 1);
        checkField(target, "followUpStatus", 0, 1);
    }

    private void checkField(CrmCustomer target, String field, Integer... legal) {
        try {
            java.lang.reflect.Field f = CrmCustomer.class.getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(target);
            if (v == null) return;
            for (Integer lv : legal) if (lv.equals(v)) return;
            throw new IllegalArgumentException(
                "字段 " + field + " 取值非法(" + v + "),请使用行按钮(转移/认领/锁定/成交)变更状态。"
            );
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("读字段失败: " + field, roe);
        }
    }
}
