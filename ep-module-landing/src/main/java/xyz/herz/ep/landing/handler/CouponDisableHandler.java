package xyz.herz.ep.landing.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.landing.entity.LandingCoupon;
import xyz.herz.ep.landing.enums.LandingDictEnums.CouponStatus;
import xyz.herz.ep.landing.jpa.LandingCouponRepository;

import java.util.List;

/**
 * 优惠券「禁用」行按钮:ENABLED(10) → DISABLED(30)。
 */
@Component
public class CouponDisableHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "coupon.disable";

    @Autowired private LandingCouponRepository couponRepo;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof LandingCoupon c)) throw new IllegalArgumentException("仅支持优惠券模板");
                if (c.getStatus() == null || c.getStatus() != CouponStatus.ENABLED.code)
                    throw new IllegalStateException("当前状态不允许禁用: " + c.getStatus());
                c.setStatus(CouponStatus.DISABLED.code);
                couponRepo.save(c);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("err: ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }
}
