package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.sale.ErpSaleOut;

import java.util.Optional;

public interface ErpSaleOutRepository extends JpaRepository<ErpSaleOut, Long> {
    Optional<ErpSaleOut> findByNo(String no);
}
