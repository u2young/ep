package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmBusinessStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmBusinessStatusRepository extends JpaRepository<CrmBusinessStatus, Long> {
    List<CrmBusinessStatus> findByTypeId(Long typeId);
}
