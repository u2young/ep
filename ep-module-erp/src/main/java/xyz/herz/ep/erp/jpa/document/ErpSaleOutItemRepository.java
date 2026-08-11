package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.sale.ErpSaleOutItem;

public interface ErpSaleOutItemRepository extends JpaRepository<ErpSaleOutItem, Long> {
}
