package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsZone;

import java.util.List;

public interface WmsZoneRepository extends JpaRepository<WmsZone, Long> {

    List<WmsZone> findByWarehouseId(Long warehouseId);

    List<WmsZone> findByCode(String code);
}
