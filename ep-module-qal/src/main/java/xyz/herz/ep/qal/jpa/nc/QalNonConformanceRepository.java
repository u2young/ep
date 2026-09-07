package xyz.herz.ep.qal.jpa.nc;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.qal.entity.nc.QalNonConformance;

import java.util.List;
import java.util.Optional;

public interface QalNonConformanceRepository extends JpaRepository<QalNonConformance, Long> {

    Optional<QalNonConformance> findByNcNo(String ncNo);

    List<QalNonConformance> findByStatus(Integer status);

    List<QalNonConformance> findBySourceInspectionNo(String sourceInspectionNo);
}
