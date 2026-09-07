package xyz.herz.ep.pay.jpa.slip;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pay.entity.slip.PaySalarySlipItem;

import java.util.List;

public interface PaySalarySlipItemRepository extends JpaRepository<PaySalarySlipItem, Long> {

    List<PaySalarySlipItem> findBySlipId(Long slipId);
}
