package xyz.herz.ep.ast.handler.movement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.entity.movement.AstAssetMovement;
import xyz.herz.ep.ast.enums.AstDictEnums.MovementStatus;

import java.util.List;

/**
 * 资产转移单生命周期行按钮处理器(提交/取消,二合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>SUBMIT:DRAFT(0) → SUBMITTED(1),同步更新 AstAsset 的 custodianName/costCenterId 快照。</li>
 *   <li>CANCEL:非已取消 → CANCELLED(2)。</li>
 * </ul>
 */
@Component
public class AstMovementLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "ast.movement.submit";
    public static final String CODE_CANCEL = "ast.movement.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof AstAssetMovement doc)) {
                fail++; sb.append("仅支持资产转移单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("转移单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applySubmit(AstAssetMovement doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != MovementStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可提交,当前状态=" + st);
        }
        AstAsset asset = doc.getAsset();
        if (asset == null) {
            throw new IllegalStateException("转移单未关联资产");
        }
        // 同步资产快照:保管人 / 成本中心(位置用字符串记录,asset.location 为 REF 不在此同步)
        if (doc.getToCustodianName() != null) {
            asset.setCustodianName(doc.getToCustodianName());
        }
        if (doc.getToCostCenterId() != null) {
            asset.setCostCenterId(doc.getToCostCenterId());
        }
        em.merge(asset);
        doc.setStatus(MovementStatus.SUBMITTED.code);
    }

    private void applyCancel(AstAssetMovement doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == MovementStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(MovementStatus.CANCELLED.code);
    }
}
