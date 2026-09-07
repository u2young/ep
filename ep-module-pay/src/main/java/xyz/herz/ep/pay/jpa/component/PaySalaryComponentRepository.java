package xyz.herz.ep.pay.jpa.component;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pay.entity.component.PaySalaryComponent;

import java.util.List;
import java.util.Optional;

public interface PaySalaryComponentRepository extends JpaRepository<PaySalaryComponent, Long> {

    Optional<PaySalaryComponent> findByCode(String code);

    List<PaySalaryComponent> findByStatus(Integer status);
}
