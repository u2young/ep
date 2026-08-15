package xyz.herz.ep.mall.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mall.facade.MallStockFacade;
import xyz.herz.ep.mall.entity.MallTradeAfterSale;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleType;
import xyz.herz.ep.mall.jpa.MallTradeAfterSaleRepository;
import xyz.herz.ep.mall.jpa.MallTradeOrderItemRepository;

import java.util.List;
import java.util.Optional;

/**
 * 售后「完成退款」行按钮处理器:50 待退款 → 60 完成。
 * <p>P0 简化:卖家已收货(40) 或 待退款(50) 均可直接完成退款到 60,
 * 50→60 为标准路径,40→60 为直通(跳过 50 中间态)。
 * <p>若 {@link MallStockFacade} 可用且售后类型为「退货退款」,
 * 完成时调用 returnStock 退回 ERP 实物库存。
 */
@Component
public class MallAfterSaleCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.aftersale.complete";
    private static final Logger log = LoggerFactory.getLogger(MallAfterSaleCompleteHandler.class);
    private static final Long DEFAULT_WAREHOUSE_ID = 1L;

    @Autowired private MallTradeAfterSaleRepository afterSaleRepo;
    @Autowired private MallTradeOrderItemRepository orderItemRepo;
    @Autowired(required = false) private MallStockFacade stockFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof MallTradeAfterSale a)) {
                    throw new IllegalArgumentException("仅支持售后单");
                }
                int cur = a.getStatus() == null ? -1 : a.getStatus();
                if (cur != AfterSaleStatus.SELLER_RECEIVE.code && cur != AfterSaleStatus.WAIT_REFUND.code) {
                    throw new IllegalStateException("当前状态不允许完成退款: " + MallAfterSaleAgreeHandler.AftersaleLabel.of(a.getStatus()));
                }
                a.setStatus(AfterSaleStatus.SUCCESS.code);
                afterSaleRepo.save(a);
                returnStockIfAvailable(a);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    /** 退货退款类型且 facade 可用时,退回实物库存;否则跳过。 */
    private void returnStockIfAvailable(MallTradeAfterSale a) {
        if (stockFacade == null) {
            log.debug("MallStockFacade 未注入,跳过售后退货入库: afterSaleNo={}", a.getNo());
            return;
        }
        Integer type = a.getType();
        if (type == null || type != AfterSaleType.RETURN_REFUND.code) return;
        if (a.getOrderItemId() == null) return;
        Optional<MallTradeOrderItem> opt = orderItemRepo.findById(a.getOrderItemId());
        if (opt.isEmpty()) return;
        MallTradeOrderItem item = opt.get();
        if (item.getSkuId() == null) return;
        int qty = item.getQuantity() == null ? 0 : item.getQuantity();
        if (qty <= 0) return;
        try {
            stockFacade.returnStock(item.getSkuId().toString(), DEFAULT_WAREHOUSE_ID, qty);
        } catch (Exception ex) {
            log.warn("returnStock 调用失败: afterSaleNo={}, skuId={}, qty={}, err={}",
                a.getNo(), item.getSkuId(), qty, ex.getMessage());
        }
    }
}
