package xyz.herz.ep.proj.jpa.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.expense.ProjExpenseClaim;

@Repository
public interface ProjExpenseClaimRepository extends JpaRepository<ProjExpenseClaim, Long> {
}
