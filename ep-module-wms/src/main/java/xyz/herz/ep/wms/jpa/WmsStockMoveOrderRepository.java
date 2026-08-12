package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsStockMoveOrder;

/** 移库作业单 Repository。 */
public interface WmsStockMoveOrderRepository extends JpaRepository<WmsStockMoveOrder, Long> {
}
