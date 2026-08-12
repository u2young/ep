package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsStockMove;
import xyz.herz.ep.wms.entity.WmsStockMoveOrder;
import xyz.herz.ep.wms.entity.WmsStockMoveOrderItem;
import xyz.herz.ep.wms.enums.WmsDictEnums.StockMoveOrderStatus;
import xyz.herz.ep.wms.jpa.WmsStockMoveRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 移库作业单完成处理器。
 * <p>状态迁移:StockMoveOrder NEW(0) / MOVING(10) → COMPLETED(20)。
 * 完成时:对每个明细写一条 {@link WmsStockMove} 流水(bizType=MOVE),
 * 记录 fromLocationId → toLocationId 的库位级变动。
 */
@Component
public class WmsStockMoveCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "wms.stockmove.complete";

    @PersistenceContext
    private EntityManager em;
    private final WmsStockMoveRepository moveRepo;

    public WmsStockMoveCompleteHandler(WmsStockMoveRepository moveRepo) {
        this.moveRepo = moveRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsStockMoveOrder order)) {
                    throw new IllegalStateException("仅支持移库作业单");
                }
                int cur = order.getStatus() == null ? StockMoveOrderStatus.NEW.code : order.getStatus();
                if (cur != StockMoveOrderStatus.NEW.code && cur != StockMoveOrderStatus.MOVING.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许完成,仅 新建/移库中 可完成");
                }

                for (WmsStockMoveOrderItem item : order.getItems()) {
                    int qty = item.getQty() == null ? 0 : item.getQty();
                    if (qty <= 0) continue;
                    WmsStockMove move = new WmsStockMove();
                    move.setFromLocationId(item.getFromLocationId());
                    move.setToLocationId(item.getToLocationId());
                    move.setSkuCode(item.getSkuCode());
                    move.setQty(qty);
                    move.setBizType("MOVE");
                    move.setRemark("移库单 " + order.getNo());
                    moveRepo.save(move);
                }

                order.setStatus(StockMoveOrderStatus.COMPLETED.code);
                em.merge(order);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_COMPLETE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private static String label(int code) {
        for (StockMoveOrderStatus a : StockMoveOrderStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
