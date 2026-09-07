package xyz.herz.ep.mfg.jpa.jobcard;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.jobcard.MfgJobCard;

import java.util.List;
import java.util.Optional;

public interface MfgJobCardRepository extends JpaRepository<MfgJobCard, Long> {

    Optional<MfgJobCard> findByNo(String no);

    /** 按工单查所有派工单。 */
    List<MfgJobCard> findByWorkOrderId(Long workOrderId);

    /** 按工单 + 状态查派工单。 */
    List<MfgJobCard> findByWorkOrderIdAndStatus(Long workOrderId, Integer status);
}
