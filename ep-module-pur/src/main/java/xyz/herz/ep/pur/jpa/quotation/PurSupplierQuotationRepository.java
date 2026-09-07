package xyz.herz.ep.pur.jpa.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pur.entity.quotation.PurSupplierQuotation;

import java.util.List;
import java.util.Optional;

/**
 * 供应商报价 Repository(参考 ERPNext Supplier Quotation 数据访问)。
 * <p>派生查询:按单号/来源询价单/供应商/是否采纳检索。
 */
public interface PurSupplierQuotationRepository extends JpaRepository<PurSupplierQuotation, Long> {

    Optional<PurSupplierQuotation> findByQuotationNo(String quotationNo);

    List<PurSupplierQuotation> findByRfqNo(String rfqNo);

    List<PurSupplierQuotation> findBySupplierCode(String supplierCode);

    List<PurSupplierQuotation> findByIsAccepted(Boolean isAccepted);
}
