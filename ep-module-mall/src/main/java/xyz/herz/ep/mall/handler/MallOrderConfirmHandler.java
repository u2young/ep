package xyz.herz.ep.mall.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.jpa.MallTradeOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单「确认收货」行按钮处理器:20 待收货 → 30 已完成,设置 confirmTime。
 */
@Component
public class MallOrderConfirmHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.order.confirm";

    @Autowired private MallTradeOrderRepository orderRepo;

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
                if (o.getStatus() == null || o.getStatus() != OrderStatus.UNRECEIVED.code) {
                    throw new IllegalStateException("当前状态不允许确认收货: " + MallOrderPayHandler.OrderLabel.of(o.getStatus()));
                }
                o.setStatus(OrderStatus.COMPLETED.code);
                o.setConfirmTime(LocalDateTime.now());
                orderRepo.save(o);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
