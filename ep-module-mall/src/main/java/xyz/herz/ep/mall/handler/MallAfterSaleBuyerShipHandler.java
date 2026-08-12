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
 * 售后「买家发货」行按钮处理器:20 同意 → 30 买家发货。
 */
@Component
public class MallAfterSaleBuyerShipHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.aftersale.buyer_ship";

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
                if (a.getStatus() == null || a.getStatus() != AfterSaleStatus.AGREED.code) {
                    throw new IllegalStateException("当前状态不允许买家发货: " + MallAfterSaleAgreeHandler.AftersaleLabel.of(a.getStatus()));
                }
                a.setStatus(AfterSaleStatus.BUYER_DELIVERY.code);
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
