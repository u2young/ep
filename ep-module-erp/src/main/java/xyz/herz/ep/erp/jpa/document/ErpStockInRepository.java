package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.document.ErpStockIn;

import java.util.Optional;

public interface ErpStockInRepository extends JpaRepository<ErpStockIn, Long> {
    Optional<ErpStockIn> findByNo(String no);
}
