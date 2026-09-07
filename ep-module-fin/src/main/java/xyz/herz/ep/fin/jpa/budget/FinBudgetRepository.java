package xyz.herz.ep.fin.jpa.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.budget.FinBudget;

import java.util.Optional;

public interface FinBudgetRepository extends JpaRepository<FinBudget, Long> {
    Optional<FinBudget> findByCostCenterIdAndFiscalYear(Long costCenterId, String fiscalYear);

    long countByCostCenterIdAndFiscalYear(Long costCenterId, String fiscalYear);
}
