package xyz.herz.ep.pur.handler.settings;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.pur.entity.settings.PurPurchaseSettings;
import xyz.herz.ep.pur.enums.PurDictEnums.EnableStatus;
import xyz.herz.ep.pur.jpa.settings.PurPurchaseSettingsRepository;

/**
 * 采购配置单例初始化器(参考 ERPNext Buying Settings + SupSupportSettingsInitializer 模板)。
 * <p>启动时若 pur_settings 表为空,插入 1 条默认记录(默认前置天数 7,
 * 最小订单金额 1000,状态启用);业务方通过 {@code findFirstByOrderByIdAsc()} 取单例。
 */
@Component
public class PurPurchaseSettingsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final PurPurchaseSettingsRepository repo;

    public PurPurchaseSettingsInitializer(PurPurchaseSettingsRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repo.count() > 0) return; // 幂等

        PurPurchaseSettings s = new PurPurchaseSettings();
        s.setDefaultBuyer("默认采购员");
        s.setDefaultLeadDays(7);
        s.setBuyerEmail("buyer@default.com");
        s.setMinOrderAmount(new java.math.BigDecimal("1000.00"));
        s.setStatus(EnableStatus.ENABLED.code);
        s.setRemark("系统自动初始化默认配置");
        repo.save(s);
    }
}
