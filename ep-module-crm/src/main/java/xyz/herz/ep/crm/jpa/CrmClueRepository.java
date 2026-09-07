package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmClue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmClueRepository extends JpaRepository<CrmClue, Long> {

    long countByFollowUpStatus(Integer followUpStatus);

    long countByTransformStatus(Integer transformStatus);

    Optional<CrmClue> findFirstByMobile(String mobile);
}
