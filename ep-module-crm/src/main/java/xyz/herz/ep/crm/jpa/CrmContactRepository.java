package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmContactRepository extends JpaRepository<CrmContact, Long> {
}
