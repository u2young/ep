package xyz.herz.ep.pur.jpa.rfq;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pur.entity.rfq.PurRequestForQuotation;

import java.util.List;
import java.util.Optional;

/**
 * 询价单 Repository(参考 ERPNext Request for Quotation 数据访问)。
 * <p>派生查询:按单号/状态/来源请购单检索。
 */
public interface PurRequestForQuotationRepository extends JpaRepository<PurRequestForQuotation, Long> {

    Optional<PurRequestForQuotation> findByRfqNo(String rfqNo);

    List<PurRequestForQuotation> findByStatus(Integer status);

    List<PurRequestForQuotation> findBySourceRequisitionNo(String sourceRequisitionNo);
}
