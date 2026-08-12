package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsStockCheck;

/** 盘点单 Repository。 */
public interface WmsStockCheckRepository extends JpaRepository<WmsStockCheck, Long> {
}
