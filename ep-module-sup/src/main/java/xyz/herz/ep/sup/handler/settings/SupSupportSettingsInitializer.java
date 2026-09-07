package xyz.herz.ep.sup.handler.settings;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;
import xyz.herz.ep.sup.jpa.settings.SupSupportSettingsRepository;

/**
 * 服务支持单例配置初始化器(参考 ERPNext Support Settings 单例)。
 * <p>启动时若 sup_settings 表为空,插入 1 条默认记录(autoAssignEnabled=false,
 * kbPublicVisible=false);业务方通过 {@code findFirstByOrderByIdAsc()} 取单例。
 */
@Component
public class SupSupportSettingsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final SupSupportSettingsRepository repo;

    public SupSupportSettingsInitializer(SupSupportSettingsRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repo.count() > 0) return; // 幂等

        SupSupportSettings s = new SupSupportSettings();
        s.setAutoAssignEnabled(Boolean.FALSE);
        s.setKbPublicVisible(Boolean.FALSE);
        s.setRemark("系统自动初始化默认配置");
        repo.save(s);
    }
}
