package xyz.herz.ep.stk.handler.settings;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.stk.entity.settings.StkStockSettings;
import xyz.herz.ep.stk.enums.StkDictEnums.EnableStatus;
import xyz.herz.ep.stk.jpa.settings.StkStockSettingsRepository;

/**
 * 库存配置单例初始化器(参考 ERPNext Stock Settings + PurPurchaseSettingsInitializer 模板)。
 * <p>启动时若 stk_settings 表为空,插入 1 条默认记录(批次/序列号默认关闭,
 * 库龄预警 90 天,状态启用);业务方通过 findFirstByOrderByIdAsc() 取单例。
 */
@Component
public class StkStockSettingsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final StkStockSettingsRepository repo;

    public StkStockSettingsInitializer(StkStockSettingsRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repo.count() > 0) return; // 幂等

        StkStockSettings s = new StkStockSettings();
        s.setDefaultWarehouseCode("WH-DEFAULT");
        s.setDefaultWarehouseName("默认仓库");
        s.setBatchEnabled(Boolean.FALSE);
        s.setSerialNoEnabled(Boolean.FALSE);
        s.setStockAgeWarningDays(90);
        s.setStatus(EnableStatus.ENABLED.code);
        s.setRemark("系统自动初始化默认配置");
        repo.save(s);
    }
}
