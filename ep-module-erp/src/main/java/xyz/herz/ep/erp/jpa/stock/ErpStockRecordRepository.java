package xyz.herz.ep.erp.jpa.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.herz.ep.erp.entity.stock.ErpStockRecord;

import java.util.List;

public interface ErpStockRecordRepository extends JpaRepository<ErpStockRecord, Long> {

    @Query("SELECT COUNT(r) FROM ErpStockRecord r WHERE r.bizType = :bizType AND r.bizId = :bizId")
    long countByBiz(@Param("bizType") int bizType, @Param("bizId") Long bizId);

    List<ErpStockRecord> findByBizTypeAndBizIdOrderByIdAsc(int bizType, Long bizId);
}
