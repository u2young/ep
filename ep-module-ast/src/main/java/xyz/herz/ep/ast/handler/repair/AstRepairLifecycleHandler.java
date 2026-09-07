package xyz.herz.ep.ast.handler.repair;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.ast.entity.repair.AstAssetRepair;
import xyz.herz.ep.ast.enums.AstDictEnums.RepairStatus;

import java.util.List;

/**
 * 资产维修单生命周期行按钮处理器(完成/取消,二合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>COMPLETE:DRAFT(0) → COMPLETED(1),维修成本记录在单据(不自动 GL 过账,
 *       维修费用科目映射可后续手工入账)。</li>
 *   <li>CANCEL:非已取消 → CANCELLED(2)。</li>
 * </ul>
 */
@Component
public class AstRepairLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "ast.repair.complete";
    public static final String CODE_CANCEL = "ast.repair.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_COMPLETE;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof AstAssetRepair doc)) {
                fail++; sb.append("仅支持资产维修单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_COMPLETE -> applyComplete(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("维修单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyComplete(AstAssetRepair doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != RepairStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可完成,当前状态=" + st);
        }
        doc.setStatus(RepairStatus.COMPLETED.code);
    }

    private void applyCancel(AstAssetRepair doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == RepairStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(RepairStatus.CANCELLED.code);
    }
}
