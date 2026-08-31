package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.crm.entity.CrmReceivablePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CrmReceivablePlanRepository extends JpaRepository<CrmReceivablePlan, Long> {
    /** 按合同查询其所有回款计划(按 periodNo 升序)。 */
    List<CrmReceivablePlan> findByContractOrderByPeriodNoAsc(CrmContract contract);

    /** 按合同 id 查询其回款计划数量。 */
    long countByContract(CrmContract contract);
}
