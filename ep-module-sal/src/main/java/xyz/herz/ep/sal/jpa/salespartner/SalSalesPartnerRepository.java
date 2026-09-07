package xyz.herz.ep.sal.jpa.salespartner;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sal.entity.salespartner.SalSalesPartner;

/**
 * 销售伙伴 Repository.
 */
public interface SalSalesPartnerRepository extends JpaRepository<SalSalesPartner, Long> {
    // 可按业务需求扩展自定义查询
}