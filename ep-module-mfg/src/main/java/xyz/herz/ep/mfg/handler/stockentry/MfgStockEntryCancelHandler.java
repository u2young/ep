package xyz.herz.ep.mfg.handler.stockentry;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntry;
import xyz.herz.ep.mfg.enums.MfgDictEnums.StockEntryStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 生产出入库单「取消」行按钮处理器。
 * <p>状态迁移:DRAFT(0) → CANCELLED(2)。
 * <p>已审核(AUDITED)单据不可直接取消——库存已联动,需另开退料/反向单冲销(参考 ERPNext 取消已提交单据需 reversal)。
 */
@Component
public class MfgStockEntryCancelHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CANCEL = "mfg.stockentry.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof MfgStockEntry doc)) {
                fail++; sb.append("仅支持出入库单; "); continue;
            }
            try {
                Integer st = doc.getStatus();
                if (st == null || st == StockEntryStatus.AUDITED.code) {
                    throw new IllegalStateException("已审核单据不可直接取消,请创建退料/反向单冲销(当前状态=" + st + ")");
                }
                if (st == StockEntryStatus.CANCELLED.code) {
                    sb.append("单据 ").append(doc.getNo()).append(" 已取消; "); continue;
                }
                doc.setStatus(StockEntryStatus.CANCELLED.code);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("单据#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CANCEL);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
