package xyz.herz.ep.wms.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.herz.ep.wms.entity.WmsStock;

import java.util.Optional;

public interface WmsStockRepository extends JpaRepository<WmsStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM WmsStock s" +
           " WHERE s.location.id = :locationId AND s.skuCode = :skuCode AND s.batchNo = :batchNo")
    Optional<WmsStock> lockByLocationSku(@Param("locationId") Long locationId,
                                         @Param("skuCode") String skuCode,
                                         @Param("batchNo") String batchNo);

    /** 默认批次 "-" 查找(加锁)。 */
    default Optional<WmsStock> findByLocationSku(Long locationId, String skuCode) {
        return lockByLocationSku(locationId, skuCode, "-");
    }
}
