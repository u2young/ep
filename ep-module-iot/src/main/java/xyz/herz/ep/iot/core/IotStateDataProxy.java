package xyz.herz.ep.iot.core;

import xyz.erupt.annotation.fun.DataProxy;

/**
 * IoT 状态机 DataProxy 基类(参考 ERP 的 ErpStateDataProxy)。
 * <p>禁止表单直接编辑 status,只能通过 @RowOperation 的「激活/启用/禁用/处理/解决/忽略」按钮改状态。
 *
 * <p>默认行为:只允许 status 为 null 时表单直接保存(即新建未指定状态);
 * 其他值一律禁止直接修改。需要放松时子类覆写 {@link #allowedDirectEditStatuses()}。
 */
public abstract class IotStateDataProxy<T> implements DataProxy<T> {

    protected abstract String stateFieldName();

    /** 允许表单直接写死的状态值数组,默认只允许 null。 */
    protected Object[] allowedDirectEditStatuses() { return new Object[]{ null }; }

    private static java.lang.reflect.Field findField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException ignore) { cur = cur.getSuperclass(); }
        }
        throw new IllegalStateException("找不到字段: " + name + " on " + c.getName());
    }

    private Object read(T t) {
        try {
            java.lang.reflect.Field f = findField(t.getClass(), stateFieldName());
            f.setAccessible(true);
            return f.get(t);
        } catch (Exception e) { throw new IllegalStateException("读状态失败: " + stateFieldName(), e); }
    }

    @Override
    public void beforeUpdate(T target) {
        Object v = read(target);
        Object[] allowed = allowedDirectEditStatuses();
        if (allowed != null) {
            for (Object a : allowed) {
                if (v == null ? a == null : v.equals(a)) return;
            }
        }
        throw new IllegalArgumentException(
            "状态字段 " + stateFieldName() + " 禁止在表单里直接修改,请通过行按钮(激活/启用/禁用/处理/解决/忽略)变更状态。"
        );
    }
}
