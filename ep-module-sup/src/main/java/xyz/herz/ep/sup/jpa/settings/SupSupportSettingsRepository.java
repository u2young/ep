package xyz.herz.ep.sup.jpa.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.settings.SupSupportSettings;

import java.util.Optional;

public interface SupSupportSettingsRepository extends JpaRepository<SupSupportSettings, Long> {

    /** 取单例配置(按 id 升序取第一条,Initializer 保证仅 1 条)。 */
    Optional<SupSupportSettings> findFirstByOrderByIdAsc();
}
