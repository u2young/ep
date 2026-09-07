package xyz.herz.ep.fin.jpa.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.payment.FinPaymentEntry;

import java.util.Optional;

public interface FinPaymentEntryRepository extends JpaRepository<FinPaymentEntry, Long> {
    Optional<FinPaymentEntry> findByNo(String no);
}
