package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.sale.ErpSaleOrderItem;

public interface ErpSaleOrderItemRepository extends JpaRepository<ErpSaleOrderItem, Long> {
}
