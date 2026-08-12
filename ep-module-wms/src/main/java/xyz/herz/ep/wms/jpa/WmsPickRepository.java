package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsPick;

import java.util.Optional;

public interface WmsPickRepository extends JpaRepository<WmsPick, Long> {

    Optional<WmsPick> findByNo(String no);
}
