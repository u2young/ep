package xyz.herz.ep.erp.jpa.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.herz.ep.erp.entity.stock.ErpStockBalance;

import java.math.BigDecimal;
import java.util.Optional;

public interface ErpStockBalanceRepository extends JpaRepository<ErpStockBalance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ErpStockBalance b" +
           " WHERE b.product.id = :productId AND b.sku.id = :skuId" +
           "   AND b.warehouse.id = :warehouseId AND b.batchNo = :batchNo")
    Optional<ErpStockBalance> lockByKey(@Param("productId") Long productId,
                                        @Param("skuId") Long skuId,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("batchNo") String batchNo);

    @Query("SELECT coalesce(sum(b.qty), 0) FROM ErpStockBalance b" +
           " WHERE b.product.id = :productId AND b.warehouse.id = :warehouseId")
    BigDecimal sumQtyByProductWarehouse(@Param("productId") Long productId,
                                         @Param("warehouseId") Long warehouseId);

    default Optional<ErpStockBalance> findByKey(Long pid, Long skuId, Long whId, String batchNo) {
        return lockByKey(pid, skuId, whId, batchNo == null ? "-" : batchNo);
    }
}
