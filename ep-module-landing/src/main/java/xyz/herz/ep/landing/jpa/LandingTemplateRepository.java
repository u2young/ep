package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.landing.entity.LandingTemplate;

@Repository
public interface LandingTemplateRepository extends JpaRepository<LandingTemplate, Long> {
}
