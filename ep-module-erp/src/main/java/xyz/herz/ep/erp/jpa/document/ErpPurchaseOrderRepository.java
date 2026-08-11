package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;

import java.util.Optional;

public interface ErpPurchaseOrderRepository extends JpaRepository<ErpPurchaseOrder, Long> {
    Optional<ErpPurchaseOrder> findByNo(String no);
}
