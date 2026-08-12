package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsLocation;

import java.util.List;

public interface WmsLocationRepository extends JpaRepository<WmsLocation, Long> {

    List<WmsLocation> findByZoneId(Long zoneId);

    List<WmsLocation> findByCode(String code);
}
