package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmBusinessRepository extends JpaRepository<CrmBusiness, Long> {
}
