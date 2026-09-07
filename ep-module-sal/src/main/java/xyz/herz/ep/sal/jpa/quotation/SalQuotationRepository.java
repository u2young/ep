package xyz.herz.ep.sal.jpa.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sal.entity.quotation.SalQuotation;

/**
 * 报价单 Repository.
 */
public interface SalQuotationRepository extends JpaRepository<SalQuotation, Long> {
    // 可按业务需求扩展自定义查询
}