package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.landing.entity.LandingAccessLog;

@Repository
public interface LandingAccessLogRepository extends JpaRepository<LandingAccessLog, Long> {
}
