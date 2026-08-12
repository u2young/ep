package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.document.ErpStockOutItem;

public interface ErpStockOutItemRepository extends JpaRepository<ErpStockOutItem, Long> {
}
