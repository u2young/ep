package xyz.herz.ep.mfg.jpa.workorder;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;

import java.util.List;
import java.util.Optional;

public interface MfgWorkOrderRepository extends JpaRepository<MfgWorkOrder, Long> {

    Optional<MfgWorkOrder> findByNo(String no);

    /** 按状态查工单(在制/未开工等)。 */
    List<MfgWorkOrder> findByStatus(Integer status);

    /** 按 BOM 查关联工单。 */
    List<MfgWorkOrder> findByBomId(Long bomId);
}
