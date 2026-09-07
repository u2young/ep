package xyz.herz.ep.pur.jpa.receipt;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;

import java.util.List;
import java.util.Optional;

/**
 * 采购收货单 Repository(参考 ERPNext Purchase Receipt 数据访问)。
 * <p>派生查询:按单号/状态/供应商/来源报价单检索。
 */
public interface PurPurchaseReceiptRepository extends JpaRepository<PurPurchaseReceipt, Long> {

    Optional<PurPurchaseReceipt> findByReceiptNo(String receiptNo);

    List<PurPurchaseReceipt> findByStatus(Integer status);

    List<PurPurchaseReceipt> findBySupplierCode(String supplierCode);

    List<PurPurchaseReceipt> findBySourceQuotationNo(String sourceQuotationNo);
}
