package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsWarehouse;

import java.util.List;
import java.util.Optional;

public interface WmsWarehouseRepository extends JpaRepository<WmsWarehouse, Long> {

    Optional<WmsWarehouse> findByCode(String code);

    List<WmsWarehouse> findByName(String name);
}
