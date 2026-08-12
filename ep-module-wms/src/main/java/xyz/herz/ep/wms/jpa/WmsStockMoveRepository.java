package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsStockMove;

import java.util.List;

public interface WmsStockMoveRepository extends JpaRepository<WmsStockMove, Long> {

    List<WmsStockMove> findBySkuCode(String skuCode);

    List<WmsStockMove> findByBizType(String bizType);
}
