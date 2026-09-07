package xyz.herz.ep.mfg.jpa.workstation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.workstation.MfgWorkstation;

import java.util.Optional;

public interface MfgWorkstationRepository extends JpaRepository<MfgWorkstation, Long> {

    Optional<MfgWorkstation> findByCode(String code);

    boolean existsByCode(String code);
}
