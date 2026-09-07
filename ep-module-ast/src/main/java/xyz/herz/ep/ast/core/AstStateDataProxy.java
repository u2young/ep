package xyz.herz.ep.ast.core;

import xyz.erupt.annotation.fun.DataProxy;

/**
 * 资产管理模块状态机 DataProxy 基类(复刻 SUP/ERP/PROJ 模板,避免跨模块依赖)。
 * <p>禁止表单直接编辑 status,只能通过 @RowOperation 的「提交/折旧/出售/报废/转移/完成」等按钮改状态。
 *
 * <p>默认行为:DRAFT(0) 状态下允许用户保存时 status 仍为 0;其他值一律禁止。
 * 需要放松时子类覆写 {@link #allowedDirectEditStatuses()}。
 */
public abstract class AstStateDataProxy<T> implements DataProxy<T> {

    protected abstract String stateFieldName();

    /** 允许表单直接写死的状态值数组,默认只允许 0(草稿)。 */
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
            "状态字段 " + stateFieldName() + " 禁止在表单里直接修改,请通过行按钮(提交/折旧/出售/报废/转移/完成)变更状态。"
        );
    }
}
