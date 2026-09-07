package xyz.herz.ep.proj.jpa.cashflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.cashflow.ProjCashFlow;

import java.util.List;

@Repository
public interface ProjCashFlowRepository extends JpaRepository<ProjCashFlow, Long> {

    /** 某项目现金流明细(供聚合报表)。 */
    List<ProjCashFlow> findByProjectId(Long projectId);
}
