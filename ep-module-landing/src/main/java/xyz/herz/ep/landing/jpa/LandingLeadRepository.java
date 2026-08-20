package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.landing.entity.LandingLead;

import java.util.List;

@Repository
public interface LandingLeadRepository extends JpaRepository<LandingLead, Long> {

    List<LandingLead> findByPageId(Long pageId);
}
