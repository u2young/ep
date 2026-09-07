package xyz.herz.ep.fin.jpa.costcenter;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;

import java.util.Optional;

public interface FinCostCenterRepository extends JpaRepository<FinCostCenter, Long> {
    Optional<FinCostCenter> findByCode(String code);
}
