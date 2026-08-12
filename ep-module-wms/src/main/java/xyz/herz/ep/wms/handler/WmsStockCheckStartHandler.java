package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsStock;
import xyz.herz.ep.wms.entity.WmsStockCheck;
import xyz.herz.ep.wms.entity.WmsStockCheckItem;
import xyz.herz.ep.wms.enums.WmsDictEnums.StockCheckStatus;
import xyz.herz.ep.wms.jpa.WmsStockRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 盘点单「开始盘点」处理器。
 * <p>状态迁移:StockCheck NEW(0) → CHECKING(10 盘点中)。
 * 开始时:对每个明细从 {@link WmsStock} 查询当前余额,回写到 item.bookQty(账面数量)。
 * 库存不存在的明细 bookQty 置 0。
 */
@Component
public class WmsStockCheckStartHandler implements OperationHandler<Object, Object> {

    public static final String CODE_START = "wms.stockcheck.start";

    @PersistenceContext
    private EntityManager em;
    private final WmsStockRepository stockRepo;

    public WmsStockCheckStartHandler(WmsStockRepository stockRepo) {
        this.stockRepo = stockRepo;
    }

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
                if (cur != StockCheckStatus.NEW.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许开始,仅 新建 可开始");
                }

                for (WmsStockCheckItem item : check.getItems()) {
                    int book = stockRepo.findByLocationSku(item.getLocationId(), item.getSkuCode())
                        .map(s -> s.getAvailableQty() == null ? 0 : s.getAvailableQty())
                        .orElse(0);
                    item.setBookQty(book);
                    em.merge(item);
                }

                check.setStatus(StockCheckStatus.CHECKING.code);
                em.merge(check);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_START);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private static String label(int code) {
        for (StockCheckStatus a : StockCheckStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
