package xyz.herz.ep.erp.jpa.document;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseIn;

import java.util.Optional;

public interface ErpPurchaseInRepository extends JpaRepository<ErpPurchaseIn, Long> {
    Optional<ErpPurchaseIn> findByNo(String no);
}
