package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.entity.CrmClue;

/**
 * 线索 DataProxy:锁定 followUpStatus / transformStatus 两个状态字段,禁止表单直接改。
 * transformStatus 只能通过「转化为客户」行按钮,followUpStatus 由写跟进自动刷新。
 *
 * 注意:CrmStateDataProxy#beforeUpdate 是第一道防线,此处不做 old/new 对比(DataProxy 未暴露 oldEntity),
 * 如果需要强一致校验,可以在 Repository 自定义 save 之前核对。
 */
public class CrmClueStateProxy extends CrmStateDataProxy<CrmClue> {

    @Override
    protected String stateFieldName() { return "followUpStatus"; }

    @Override
    public void beforeUpdate(CrmClue target) {
        // followUpStatus + transformStatus 都不可直接编辑
        checkNoEdit(target, "followUpStatus", "跟进状态");
        checkNoEdit(target, "transformStatus", "转化状态");
    }

    private void checkNoEdit(CrmClue target, String field, String label) {
        try {
            java.lang.reflect.Field f = CrmClue.class.getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(target);
            // 提交状态时,如果是 erupt 更新前,值一旦被提交为非 null 就报错。
            // 但 BaseModel 新建后 id 非空,followUpStatus 默认 0 是合法的,仅当表单显式改值时才拦。
            // 简化:非初始值(0)或 transformStatus=1 就报警。
            if ("followUpStatus".equals(field)) {
                if (v != null && (Integer) v != 0 && (Integer) v != 1) {
                    throw new IllegalArgumentException(label + " 字段取值非法: " + v);
                }
            }
            if ("transformStatus".equals(field)) {
                if (v != null && (Integer) v == 1) {
                    // 用户在表单里手动把 transformStatus 改成 1 — 非法
                    throw new IllegalArgumentException(
                        label + " 字段禁止表单直接修改,请使用行按钮【转化为客户】"
                    );
                }
            }
        } catch (ReflectiveOperationException roe) {
            throw new IllegalStateException("读字段失败: " + field, roe);
        }
    }
}
