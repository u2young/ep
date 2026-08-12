package xyz.herz.ep.mall.core;

import xyz.erupt.annotation.fun.DataProxy;

/**
 * 商城状态机 DataProxy 基类(参考 ErpStateDataProxy,独立实现避免跨模块依赖)。
 * <p>禁止表单直接编辑状态字段,只能通过 @RowOperation 的行按钮(付款/发货/确认收货/取消/
 * 售后同意/拒绝/完成等)变更状态。
 *
 * <p>默认行为:仅允许状态字段的初始值(0 或 null)被表单直接写入,其它值一律禁止。
 * 子类可覆写 {@link #allowedDirectEditStatuses()} 放宽。
 */
public abstract class MallStateDataProxy<T> implements DataProxy<T> {

    /** 锁定的状态字段名(单字段)。 */
    protected abstract String stateFieldName();

    /** 允许表单直接写死的状态值数组,默认只允许 0 / null(初始态)。 */
    protected Object[] allowedDirectEditStatuses() { return new Object[]{ 0, null }; }

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
            "状态字段 " + stateFieldName() + " 禁止在表单里直接修改,请通过行按钮变更状态。"
        );
    }
}
