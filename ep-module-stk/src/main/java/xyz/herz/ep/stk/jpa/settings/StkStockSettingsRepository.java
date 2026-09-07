package xyz.herz.ep.stk.jpa.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.stk.entity.settings.StkStockSettings;

import java.util.Optional;

/**
 * 库存配置 Repository(单例,通过 findFirstByOrderByIdAsc 取全局唯一配置)。
 */
public interface StkStockSettingsRepository extends JpaRepository<StkStockSettings, Long> {

    Optional<StkStockSettings> findFirstByOrderByIdAsc();
}
