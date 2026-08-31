package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.EruptButtonHandler;
import xyz.herz.ep.wms.entity.WmsStock;
import xyz.herz.ep.wms.entity.WmsStockMoveOrder;
import xyz.herz.ep.wms.entity.WmsStockMoveOrderItem;
import xyz.herz.ep.wms.jpa.WmsStockMoveOrderRepository;
import xyz.herz.ep.wms.jpa.WmsStockRepository;

import java.util.List;

/**
 * WMS 移库作业 BUTTON: 推荐移库数量处理器。
 * <p>
 * 用法: 在 {@link WmsStockMoveOrder} 编辑页点「推荐移库数」按钮,
 * 自动根据每条明细的 fromLocationId + skuCode 查询源库位实际可用库存 WmsStock,
 * 把每条明细的 qty clamp 到 min(可用库存, 原 qty)。
 * <p>
 * 典型场景: 用户先拍脑袋填大概的移库数,点推荐按钮后系统按真实库存校正。
 *
 * @author ep-wms
 */
@Component
public class WmsMoveRecommendButtonHandler implements EruptButtonHandler<WmsStockMoveOrder> {

    private final WmsStockRepository stockRepo;
    private final WmsStockMoveOrderRepository moveOrderRepo;

    public WmsMoveRecommendButtonHandler(WmsStockRepository stockRepo,
                                         WmsStockMoveOrderRepository moveOrderRepo) {
        this.stockRepo = stockRepo;
        this.moveOrderRepo = moveOrderRepo;
    }

    @Override
    public String click(WmsStockMoveOrder order, String[] params) {
        return exec(order);
    }

    /**
     * 核心逻辑 (可直接被测试调用,跳过 click 接口层)。
     *
     * @param order 移库作业单 (必须已持久化且有 items 关联)
     * @return 结果提示文案 (含处理条数)
     */
    @Transactional
    public String exec(WmsStockMoveOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("移库作业单不能为空");
        }
        List<WmsStockMoveOrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            return "无明细,无需处理";
        }

        for (WmsStockMoveOrderItem item : items) {
            Long fromLoc = item.getFromLocationId();
            if (fromLoc == null) {
                throw new IllegalArgumentException(
                    "明细 id=" + item.getId() + "(sku=" + item.getSkuCode() + ") "
                        + "fromLocationId 不能为空(需指定源库位才能查可用库存)");
            }
            String sku = item.getSkuCode();
            int avail = stockRepo.findByLocationSku(fromLoc, sku)
                .map(WmsStock::getAvailableQty)
                .orElse(0);
            avail = Math.max(avail, 0);
            int req = item.getQty() == null ? 0 : item.getQty();
            req = Math.max(req, 0);
            int clamped = Math.min(avail, req);
            item.setQty(clamped);
        }

        // CascadeType.ALL 级联更新 items 的 qty 字段
        moveOrderRepo.save(order);

        return "处理 " + items.size() + " 条明细:已根据源库位可用库存 clamp 移库数量";
    }
}
