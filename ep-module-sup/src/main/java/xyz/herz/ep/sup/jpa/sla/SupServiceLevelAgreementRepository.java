package xyz.herz.ep.sup.jpa.sla;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.sla.SupServiceLevelAgreement;

import java.util.List;

public interface SupServiceLevelAgreementRepository
        extends JpaRepository<SupServiceLevelAgreement, Long> {

    /** 客户专属 SLA(按客户 ID)。 */
    List<SupServiceLevelAgreement> findByCustomer_IdAndStatus(Long customerId, Integer status);

    /** 全公司默认 SLA(customer 为 null)。 */
    List<SupServiceLevelAgreement> findByCustomerIsNullAndStatus(Integer status);

    List<SupServiceLevelAgreement> findByStatus(Integer status);
}
