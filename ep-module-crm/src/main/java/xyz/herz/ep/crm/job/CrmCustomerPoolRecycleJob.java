package xyz.herz.ep.crm.job;

import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.entity.CrmCustomerPoolConfig;
import xyz.herz.ep.crm.jpa.CrmCustomerPoolConfigRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公海自动回收定时任务。
 *
 * erupt-job 没有注解,用 Spring @Scheduled 实现(需要在启动类加 {@code @EnableScheduling})。
 * 配置细节:
 *  - cron:每天 03:00 执行
 *  - 逻辑:查询 CrmCustomerPoolConfig 的 contactExpireDays / dealExpireDays,
 *    满足 非空 ownerUserId + 未锁定 + 未成交 + 超过配置天数未跟进/未成交 → 释放为公海。
 */
@Component
public class CrmCustomerPoolRecycleJob {

    @Resource private CrmCustomerRepository customerRepo;
    @Resource private CrmCustomerPoolConfigRepository poolRepo;

    @Scheduled(cron = "0 0 3 * * ?")
    public void run() {
        CrmCustomerPoolConfig cfg = poolRepo.findAll().stream().findFirst().orElse(null);
        if (cfg == null || cfg.getEnabled() == null || !cfg.getEnabled()) return;
        int contactDays = cfg.getContactExpireDays() == null ? 0 : cfg.getContactExpireDays();
        int dealDays = cfg.getDealExpireDays() == null ? 0 : cfg.getDealExpireDays();
        if (contactDays <= 0 && dealDays <= 0) return;
        LocalDateTime contactExpire = contactDays > 0 ? LocalDateTime.now().minusDays(contactDays) : LocalDateTime.MAX;
        LocalDateTime dealExpire = dealDays > 0 ? LocalDateTime.now().minusDays(dealDays) : LocalDateTime.MAX;
        List<CrmCustomer> list = customerRepo.findExpireRecycle(contactExpire, dealExpire);
        for (CrmCustomer c : list) {
            c.setOwnerUserId(null);
            c.setOwnerTime(null);
        }
        if (!list.isEmpty()) customerRepo.saveAll(list);
    }
}
