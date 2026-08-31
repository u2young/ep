package xyz.herz.ep.boot.print;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EruptPrintTemplateRepository extends JpaRepository<EruptPrintTemplate, Long> {
    Optional<EruptPrintTemplate> findByCode(String code);
    long countByBizModule(String bizModule);
}
