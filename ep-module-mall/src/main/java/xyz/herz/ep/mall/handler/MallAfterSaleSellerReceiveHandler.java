package xyz.herz.ep.mall.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mall.entity.MallTradeAfterSale;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleStatus;
import xyz.herz.ep.mall.jpa.MallTradeAfterSaleRepository;

import java.util.List;

/**
 * 售后「卖家收货」行按钮处理器:30 买家发货 → 40 卖家收货。
 */
@Component
public class MallAfterSaleSellerReceiveHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.aftersale.seller_receive";

    @Autowired private MallTradeAfterSaleRepository afterSaleRepo;

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
                if (a.getStatus() == null || a.getStatus() != AfterSaleStatus.BUYER_DELIVERY.code) {
                    throw new IllegalStateException("当前状态不允许卖家收货: " + MallAfterSaleAgreeHandler.AftersaleLabel.of(a.getStatus()));
                }
                a.setStatus(AfterSaleStatus.SELLER_RECEIVE.code);
                afterSaleRepo.save(a);
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
