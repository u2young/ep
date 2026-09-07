package xyz.herz.ep.pay.jpa.structure;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructure;

import java.util.List;
import java.util.Optional;

public interface PaySalaryStructureRepository extends JpaRepository<PaySalaryStructure, Long> {

    Optional<PaySalaryStructure> findByCode(String code);

    List<PaySalaryStructure> findByStatus(Integer status);
}
