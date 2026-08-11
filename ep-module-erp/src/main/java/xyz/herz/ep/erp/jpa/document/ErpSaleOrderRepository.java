package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.sale.ErpSaleOrder;

import java.util.Optional;

public interface ErpSaleOrderRepository extends JpaRepository<ErpSaleOrder, Long> {
    Optional<ErpSaleOrder> findByNo(String no);
}
