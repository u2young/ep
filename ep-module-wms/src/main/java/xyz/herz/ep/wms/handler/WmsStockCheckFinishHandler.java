package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsStockCheck;
import xyz.herz.ep.wms.entity.WmsStockCheckItem;
import xyz.herz.ep.wms.enums.WmsDictEnums.StockCheckStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 盘点单「完成盘点」处理器。
 * <p>状态迁移:StockCheck CHECKING(10) → COMPLETED(20,无差异) / HAS_DIFF(30,有差异)。
 * 完成时:对每个明细计算 diffQty = actualQty - bookQty;若任一明细 diffQty ≠ 0 则状态为 HAS_DIFF。
 */
@Component
public class WmsStockCheckFinishHandler implements OperationHandler<Object, Object> {

    public static final String CODE_FINISH = "wms.stockcheck.finish";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsStockCheck check)) {
                    throw new IllegalStateException("仅支持盘点单");
                }
                int cur = check.getStatus() == null ? StockCheckStatus.NEW.code : check.getStatus();
                if (cur != StockCheckStatus.CHECKING.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许完成,仅 盘点中 可完成");
                }

                boolean hasDiff = false;
                for (WmsStockCheckItem item : check.getItems()) {
                    int book = item.getBookQty() == null ? 0 : item.getBookQty();
                    int actual = item.getActualQty() == null ? 0 : item.getActualQty();
                    int diff = actual - book;
                    item.setDiffQty(diff);
                    em.merge(item);
                    if (diff != 0) hasDiff = true;
                }

                check.setStatus(hasDiff ? StockCheckStatus.HAS_DIFF.code : StockCheckStatus.COMPLETED.code);
                em.merge(check);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_FINISH);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private static String label(int code) {
        for (StockCheckStatus a : StockCheckStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
