package xyz.herz.ep.mall.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mall.facade.MallStockFacade;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.jpa.MallTradeOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单「取消」行按钮处理器:0 待付款 / 10 待发货 → 40 已取消,设置 cancelTime。
 * <p>若 {@link MallStockFacade} 可用,取消时调用 unlockStock 解锁已锁定库存。
 */
@Component
public class MallOrderCancelHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.order.cancel";
    private static final Logger log = LoggerFactory.getLogger(MallOrderCancelHandler.class);
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
                int cur = o.getStatus() == null ? OrderStatus.UNPAID.code : o.getStatus();
                if (cur != OrderStatus.UNPAID.code && cur != OrderStatus.UNDELIVERED.code) {
                    throw new IllegalStateException("当前状态不允许取消: " + MallOrderPayHandler.OrderLabel.of(cur));
                }
                o.setStatus(OrderStatus.CANCELED.code);
                o.setCancelTime(LocalDateTime.now());
                orderRepo.save(o);
                unlockStockIfAvailable(o);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    /** 取消时解锁库存;facade 不可用时只记录日志跳过。 */
    private void unlockStockIfAvailable(MallTradeOrder o) {
        if (stockFacade == null) {
            log.debug("MallStockFacade 未注入,跳过取消解锁库存: orderNo={}", o.getNo());
            return;
        }
        if (o.getItems() == null) return;
        for (MallTradeOrderItem item : o.getItems()) {
            if (item.getSkuId() == null) continue;
            int qty = item.getLockStock() == null ? 0 : item.getLockStock();
            if (qty <= 0) continue;
            try {
                stockFacade.unlockStock(item.getSkuId().toString(), DEFAULT_WAREHOUSE_ID, qty);
            } catch (Exception ex) {
                log.warn("unlockStock 调用失败: orderNo={}, skuId={}, qty={}, err={}",
                    o.getNo(), item.getSkuId(), qty, ex.getMessage());
            }
        }
    }
}
