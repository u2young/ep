package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.landing.entity.LandingPage;

import java.util.Optional;

@Repository
public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {

    Optional<LandingPage> findBySlug(String slug);

    Optional<LandingPage> findByShortCode(String shortCode);

    /** PV 原子自增(每次访问 +1)。 */
    @Modifying
    @Query("UPDATE LandingPage p SET p.pvCount = p.pvCount + 1 WHERE p.id = :id")
    int incrementPv(@Param("id") Long id);

    /** UV 原子自增(首次访问 +1,由调用方保证去重)。 */
    @Modifying
    @Query("UPDATE LandingPage p SET p.uvCount = p.uvCount + 1 WHERE p.id = :id")
    int incrementUv(@Param("id") Long id);
}
