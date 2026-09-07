package xyz.herz.ep.pur.jpa.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pur.entity.settings.PurPurchaseSettings;

import java.util.Optional;

/**
 * 采购配置 Repository(单例,id 固定 1L)。
 * <p>通过 findFirstByOrderByIdAsc 获取全局唯一配置。
 */
public interface PurPurchaseSettingsRepository extends JpaRepository<PurPurchaseSettings, Long> {

    Optional<PurPurchaseSettings> findFirstByOrderByIdAsc();
}
