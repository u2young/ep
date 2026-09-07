package xyz.herz.ep.fin.jpa.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;

import java.util.List;
import java.util.Optional;

public interface FinSalesInvoiceRepository extends JpaRepository<FinSalesInvoice, Long> {
    Optional<FinSalesInvoice> findByNo(String no);

    List<FinSalesInvoice> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
