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
 * 售后「拒绝」行按钮处理器:10 申请 → 70 拒绝。
 */
@Component
public class MallAfterSaleRejectHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "mall.aftersale.reject";

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
                if (a.getStatus() == null || a.getStatus() != AfterSaleStatus.APPLY.code) {
                    throw new IllegalStateException("当前状态不允许拒绝: " + MallAfterSaleAgreeHandler.AftersaleLabel.of(a.getStatus()));
                }
                a.setStatus(AfterSaleStatus.REJECTED.code);
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
