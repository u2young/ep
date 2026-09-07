package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.entity.CrmClue;
import xyz.herz.ep.crm.jpa.CrmClueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线索 DataProxy:锁定 followUpStatus / transformStatus 两个状态字段,禁止表单直接改。
 * transformStatus 只能通过「转化为客户」行按钮,followUpStatus 由写跟进自动刷新。
 *
 * <p>新增线索时校验手机号去重(未转化线索中不允许相同手机号)。
 */
@Component
public class CrmClueStateProxy extends CrmStateDataProxy<CrmClue> {

    @Autowired private CrmClueRepository clueRepo;

    @Override
    protected String stateFieldName() { return "followUpStatus"; }

    @Override
    public void beforeAdd(CrmClue target) {
        if (target.getMobile() != null && !target.getMobile().isBlank()) {
            clueRepo.findFirstByMobile(target.getMobile()).ifPresent(existing -> {
                // 仅已有线索未转化时才判重(已转化的线索不再占用)
                if (existing.getTransformStatus() == null || existing.getTransformStatus() == 0) {
                    throw new IllegalArgumentException(
                        "手机号 " + target.getMobile() + " 已存在未转化线索(ID=" + existing.getId() + "),请勿重复导入。"
                    );
                }
            });
        }
    }

    @Override
    public void beforeUpdate(CrmClue target) {
        checkNoEdit(target, "followUpStatus", "跟进状态");
        checkNoEdit(target, "transformStatus", "转化状态");
    }

    private void checkNoEdit(CrmClue target, String field, String label) {
        try {
            java.lang.reflect.Field f = CrmClue.class.getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(target);
            if ("followUpStatus".equals(field)) {
                if (v != null && (Integer) v != 0 && (Integer) v != 1) {
                    throw new IllegalArgumentException(label + " 字段取值非法: " + v);
                }
            }
            if ("transformStatus".equals(field)) {
                if (v != null && (Integer) v == 1) {
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
