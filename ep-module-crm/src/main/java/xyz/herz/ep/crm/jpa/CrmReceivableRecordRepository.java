package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmReceivableRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmReceivableRecordRepository extends JpaRepository<CrmReceivableRecord, Long> {
}
