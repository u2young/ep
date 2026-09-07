package xyz.herz.ep.stk.jpa.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;

import java.util.List;
import java.util.Optional;

/**
 * 库存出入库单 Repository(参考 ERPNext Stock Entry 数据访问)。
 */
public interface StkStockEntryRepository extends JpaRepository<StkStockEntry, Long> {

    Optional<StkStockEntry> findByEntryNo(String entryNo);

    List<StkStockEntry> findByStatus(Integer status);

    List<StkStockEntry> findByEntryType(Integer entryType);

    List<StkStockEntry> findBySourceWarehouseCode(String sourceWarehouseCode);

    List<StkStockEntry> findByTargetWarehouseCode(String targetWarehouseCode);
}
