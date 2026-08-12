package xyz.herz.ep.mall.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.common.facade.MallStockFacade;
import xyz.herz.ep.mall.entity.MallPayOrder;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.PayStatus;
import xyz.herz.ep.mall.jpa.MallPayOrderRepository;
import xyz.herz.ep.mall.jpa.MallTradeOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单「付款」行按钮处理器:0 待付款 → 10 待发货,设置 payTime,并联动生成已支付的支付单。
 * <p>若 {@link MallStockFacade} 可用,付款时调用 lockStock 锁定 ERP 库存,
 * 并把锁定量回写到订单明细的 lockStock 字段(取消时按此解锁)。
 */
@Component
public class MallOrderPayHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.order.pay";
    private static final Logger log = LoggerFactory.getLogger(MallOrderPayHandler.class);
    private static final Long DEFAULT_WAREHOUSE_ID = 1L;

    @Autowired private MallTradeOrderRepository orderRepo;
    @Autowired private MallPayOrderRepository payOrderRepo;
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
                if (o.getStatus() == null || o.getStatus() != OrderStatus.UNPAID.code) {
                    throw new IllegalStateException("当前状态不允许付款: " + OrderLabel.of(o.getStatus()));
                }
                o.setStatus(OrderStatus.UNDELIVERED.code);
                o.setPayTime(LocalDateTime.now());
                orderRepo.save(o);

                MallPayOrder pay = new MallPayOrder();
                pay.setNo("PAY-" + o.getId());
                pay.setOrderId(o.getId());
                pay.setAmount(o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount());
                pay.setStatus(PayStatus.PAID.code);
                pay.setPayTime(o.getPayTime());
                pay.setPayType("SIMULATED");
                payOrderRepo.save(pay);
                lockStockIfAvailable(o);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    /** 付款时锁定库存;facade 不可用时只记录日志跳过。 */
    private void lockStockIfAvailable(MallTradeOrder o) {
        if (stockFacade == null) {
            log.debug("MallStockFacade 未注入,跳过付款锁库存: orderNo={}", o.getNo());
            return;
        }
        if (o.getItems() == null) return;
        for (MallTradeOrderItem item : o.getItems()) {
            if (item.getSkuId() == null) continue;
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0) continue;
            try {
                stockFacade.lockStock(item.getSkuId().toString(), DEFAULT_WAREHOUSE_ID, qty);
                item.setLockStock(qty);
            } catch (Exception ex) {
                log.warn("lockStock 调用失败: orderNo={}, skuId={}, qty={}, err={}",
                    o.getNo(), item.getSkuId(), qty, ex.getMessage());
            }
        }
    }

    static final class OrderLabel {
        static String of(Integer code) {
            if (code == null) return "null";
            for (OrderStatus s : OrderStatus.values()) if (s.code == code) return s.label;
            return "UNKNOWN(" + code + ")";
        }
    }
}
