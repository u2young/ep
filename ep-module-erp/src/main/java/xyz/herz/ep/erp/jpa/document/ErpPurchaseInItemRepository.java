package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseInItem;

public interface ErpPurchaseInItemRepository extends JpaRepository<ErpPurchaseInItem, Long> {
}
