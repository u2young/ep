package xyz.herz.ep.pay.jpa.slip;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;

import java.util.List;
import java.util.Optional;

public interface PaySalarySlipRepository extends JpaRepository<PaySalarySlip, Long> {

    Optional<PaySalarySlip> findBySlipNo(String slipNo);

    List<PaySalarySlip> findByStatus(Integer status);

    List<PaySalarySlip> findByEmployeeId(Long employeeId);

    List<PaySalarySlip> findByPostingMonth(String postingMonth);
}
