package xyz.herz.ep.boot.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EruptReportRepository extends JpaRepository<EruptReportEntity, Long> {
    Optional<EruptReportEntity> findByCode(String code);
    long countByBizModule(String bizModule);
}
