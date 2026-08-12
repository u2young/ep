package xyz.herz.ep.mall.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.common.facade.MallStockFacade;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.jpa.MallTradeOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单「发货」行按钮处理器:10 待发货 → 20 待收货,设置 shipTime。
 * <p>若 {@link MallStockFacade} 可用,发货时同步调用 deductStock 扣减 ERP 实物库存。
 */
@Component
public class MallOrderShipHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.order.ship";
    private static final Logger log = LoggerFactory.getLogger(MallOrderShipHandler.class);
    /** Mall 不直接持有仓库信息,跨模块调用时使用默认仓库 ID。 */
    private static final Long DEFAULT_WAREHOUSE_ID = 1L;

    @Autowired private MallTradeOrderRepository orderRepo;
    @Autowired(required = false) private MallStockFacade stockFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof MallTradeOrder o)) {
                    throw new IllegalArgumentException("仅支持交易订单");
                }
                if (o.getStatus() == null || o.getStatus() != OrderStatus.UNDELIVERED.code) {
                    throw new IllegalStateException("当前状态不允许发货: " + MallOrderPayHandler.OrderLabel.of(o.getStatus()));
                }
                o.setStatus(OrderStatus.UNRECEIVED.code);
                o.setShipTime(LocalDateTime.now());
                orderRepo.save(o);
                deductStockIfAvailable(o);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    /** 发货时同步扣减 ERP 实物库存;facade 不可用时只记录日志跳过。 */
    private void deductStockIfAvailable(MallTradeOrder o) {
        if (stockFacade == null) {
            log.debug("MallStockFacade 未注入,跳过发货扣库存: orderNo={}", o.getNo());
            return;
        }
        if (o.getItems() == null) return;
        for (MallTradeOrderItem item : o.getItems()) {
            if (item.getSkuId() == null) continue;
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0) continue;
            try {
                stockFacade.deductStock(item.getSkuId().toString(), DEFAULT_WAREHOUSE_ID, qty);
            } catch (Exception ex) {
                log.warn("deductStock 调用失败: orderNo={}, skuId={}, qty={}, err={}",
                    o.getNo(), item.getSkuId(), qty, ex.getMessage());
            }
        }
    }
}
