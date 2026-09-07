package xyz.herz.ep.fin.jpa.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;

import java.util.List;
import java.util.Optional;

public interface FinPurchaseInvoiceRepository extends JpaRepository<FinPurchaseInvoice, Long> {
    Optional<FinPurchaseInvoice> findByNo(String no);

    List<FinPurchaseInvoice> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
