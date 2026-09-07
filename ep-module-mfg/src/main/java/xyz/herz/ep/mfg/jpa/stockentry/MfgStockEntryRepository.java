package xyz.herz.ep.mfg.jpa.stockentry;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntry;

import java.util.List;
import java.util.Optional;

public interface MfgStockEntryRepository extends JpaRepository<MfgStockEntry, Long> {

    Optional<MfgStockEntry> findByNo(String no);

    /** 按工单查所有出入库单。 */
    List<MfgStockEntry> findByWorkOrderId(Long workOrderId);

    /** 按工单 + 单据类型查(如某工单的所有领料单)。 */
    List<MfgStockEntry> findByWorkOrderIdAndTtype(Long workOrderId, Integer ttype);

    /** 按状态查。 */
    List<MfgStockEntry> findByStatus(Integer status);
}
