package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmContract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmContractRepository extends JpaRepository<CrmContract, Long> {
}
