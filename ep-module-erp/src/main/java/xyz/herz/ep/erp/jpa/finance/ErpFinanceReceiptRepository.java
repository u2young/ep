package xyz.herz.ep.erp.jpa.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.finance.ErpFinanceReceipt;

import java.util.Optional;

public interface ErpFinanceReceiptRepository extends JpaRepository<ErpFinanceReceipt, Long> {
    Optional<ErpFinanceReceipt> findByNo(String no);
}
