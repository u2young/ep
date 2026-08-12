package xyz.herz.ep.erp.jpa.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.finance.ErpFinancePayment;

import java.util.Optional;

public interface ErpFinancePaymentRepository extends JpaRepository<ErpFinancePayment, Long> {
    Optional<ErpFinancePayment> findByNo(String no);
}
