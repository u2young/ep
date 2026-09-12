package xyz.herz.ep.landing.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.landing.entity.LandingSeckill;
import xyz.herz.ep.landing.enums.LandingDictEnums.SeckillStatus;
import xyz.herz.ep.landing.jpa.LandingSeckillRepository;

import java.util.List;

/**
 * 秒杀活动「结束」行按钮:ACTIVE(10) → ENDED(20)。
 */
@Component
public class SeckillEndHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "seckill.end";

    @Autowired private LandingSeckillRepository seckillRepo;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof LandingSeckill s)) throw new IllegalArgumentException("仅支持秒杀活动");
                if (s.getStatus() == null || s.getStatus() != SeckillStatus.ACTIVE.code)
                    throw new IllegalStateException("当前状态不允许结束: " + s.getStatus());
                s.setStatus(SeckillStatus.ENDED.code);
                seckillRepo.save(s);
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
