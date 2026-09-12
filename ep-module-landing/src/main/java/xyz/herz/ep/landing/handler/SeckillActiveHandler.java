package xyz.herz.ep.landing.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.landing.entity.LandingSeckill;
import xyz.herz.ep.landing.enums.LandingDictEnums.SeckillStatus;
import xyz.herz.ep.landing.jpa.LandingSeckillRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动「开始」行按钮:DRAFT(0) → ACTIVE(10)。
 * <p>校验:当前状态为草稿,且开始时间在当前时间之后。
 */
@Component
public class SeckillActiveHandler implements OperationHandler<Object, Object> {

    public static final String CODE = "seckill.active";

    @Autowired private LandingSeckillRepository seckillRepo;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof LandingSeckill s)) throw new IllegalArgumentException("仅支持秒杀活动");
                if (s.getStatus() != null && s.getStatus() != SeckillStatus.DRAFT.code)
                    throw new IllegalStateException("当前状态不允许开始: " + s.getStatus());
                if (s.getStartTime() != null && s.getStartTime().isBefore(LocalDateTime.now()))
                    throw new IllegalStateException("开始时间不能早于当前时间");
                s.setStatus(SeckillStatus.ACTIVE.code);
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
