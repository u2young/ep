package xyz.herz.ep.mfg.jpa.subcontract;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.subcontract.MfgSubcontractingOrder;

import java.util.List;
import java.util.Optional;

public interface MfgSubcontractingOrderRepository extends JpaRepository<MfgSubcontractingOrder, Long> {

    Optional<MfgSubcontractingOrder> findByNo(String no);

    /** 按工单查委外单。 */
    List<MfgSubcontractingOrder> findByWorkOrderId(Long workOrderId);

    /** 按供应商查委外单。 */
    List<MfgSubcontractingOrder> findBySupplierId(Long supplierId);

    /** 按状态查委外单。 */
    List<MfgSubcontractingOrder> findByStatus(Integer status);
}
