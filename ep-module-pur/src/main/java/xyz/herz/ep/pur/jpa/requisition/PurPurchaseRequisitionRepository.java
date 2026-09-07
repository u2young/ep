package xyz.herz.ep.pur.jpa.requisition;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pur.entity.requisition.PurPurchaseRequisition;

import java.util.List;
import java.util.Optional;

/**
 * 请购单 Repository(参考 ERPNext Material Request 数据访问)。
 * <p>派生查询:按单号/状态/请购人/来源检索。
 */
public interface PurPurchaseRequisitionRepository extends JpaRepository<PurPurchaseRequisition, Long> {

    Optional<PurPurchaseRequisition> findByRequisitionNo(String requisitionNo);

    List<PurPurchaseRequisition> findByStatus(Integer status);

    List<PurPurchaseRequisition> findByRequester(String requester);
}
