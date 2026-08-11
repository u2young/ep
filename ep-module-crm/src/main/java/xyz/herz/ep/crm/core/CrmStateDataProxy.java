package xyz.herz.ep.crm.core;

import xyz.erupt.annotation.fun.DataProxy;

/**
 * CRM 状态机 DataProxy 基类:统一在 {@code beforeUpdate} 里锁定状态字段,
 * 禁止用户通过 Erupt 表单直接编辑 status,必须通过 {@code @RowOperation} 按钮触发。
 *
 * <p>用法:实体类 {@code @Erupt(dataProxy = XxxStateProxy.class)},实现 {@link #stateFieldName()} 返回
 * 该实体的状态字段名(例如 {@code "followUpStatus"}),以及 {@link #allowedDirectEditStatuses()} 返回
 * 允许直接编辑的极端值(null 或空数组=全部禁止,最严格)。
 */
public abstract class CrmStateDataProxy<T> implements DataProxy<T> {

    protected abstract String stateFieldName();

    /** 返回允许在表单里直接改的状态值(默认全部不允许,走 @RowOperation)。返回 null = 全部不允许。 */
    protected Object[] allowedDirectEditStatuses() { return null; }

    private Object read(T target) {
        try {
            java.lang.reflect.Field f = findField(target.getClass(), stateFieldName());
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new IllegalStateException("读状态字段失败: " + stateFieldName(), e);
        }
    }

    private Object readOld(Object old) {
        try {
            java.lang.reflect.Field f = findField(old.getClass(), stateFieldName());
            f.setAccessible(true);
            return f.get(old);
        } catch (Exception e) { return null; }
    }

    private static java.lang.reflect.Field findField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException ignore) { cur = cur.getSuperclass(); }
        }
        throw new IllegalStateException("找不到字段: " + name + " on " + c.getName());
    }

    @Override
    public void beforeUpdate(T target) {
        Object newVal = read(target);
        // Erupt DataProxy 没有直接拿到 old entity 的公开入口,这里做一个"禁止编辑"弱校验:
        // 只要状态字段非 null/非默认值,就认为用户直接改了,抛错。真正严格校验需要在持久化前
        // 通过 @EntityListeners 或 Service 再核对一次,DataProxy 是第一道防线。
        Object[] allowed = allowedDirectEditStatuses();
        if (newVal != null && allowed != null) {
            for (Object a : allowed) if (newVal.equals(a)) return;
        } else if (newVal == null) {
            return;
        }
        // 兼容 erupt DataProxy#beforeUpdate 不提供 oldEntity,我们仅做"表单内不要编辑 status 字段"提示。
        throw new IllegalArgumentException(
            "状态字段 " + stateFieldName() + " 禁止在表单里直接修改,请使用行按钮(转化/推进/赢单/输单)变更状态。"
        );
    }
}
