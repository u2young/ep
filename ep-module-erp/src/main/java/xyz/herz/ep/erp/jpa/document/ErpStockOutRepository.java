package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.document.ErpStockOut;

import java.util.Optional;

public interface ErpStockOutRepository extends JpaRepository<ErpStockOut, Long> {
    Optional<ErpStockOut> findByNo(String no);
}
