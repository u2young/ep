package xyz.herz.ep.stk.jpa.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.stk.entity.reconciliation.StkStockReconciliation;

import java.util.List;
import java.util.Optional;

/**
 * 库存盘点单 Repository(参考 ERPNext Stock Reconciliation 数据访问)。
 */
public interface StkStockReconciliationRepository extends JpaRepository<StkStockReconciliation, Long> {

    Optional<StkStockReconciliation> findByReconciliationNo(String reconciliationNo);

    List<StkStockReconciliation> findByStatus(Integer status);

    List<StkStockReconciliation> findByWarehouseCode(String warehouseCode);
}
