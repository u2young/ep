package xyz.herz.ep.fin.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.fin.enums.FinDictEnums.InvoiceStatus;
import xyz.herz.ep.fin.jpa.invoice.FinPurchaseInvoiceRepository;
import xyz.herz.ep.fin.jpa.invoice.FinSalesInvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务发票门面默认实现。
 * <p>由 erp/mall/crm 等业务模块通过 {@code @Autowired(required=false)} 注入调用,
 * 触发生成应收/应付发票(初始 DRAFT 状态,由 fin 模块自身 Submit 行按钮触发 GL 凭证)。
 *
 * <p>注意:本类不触发 GL 凭证(GL 由 invoice Submit 行按钮触发,经 FinPostingService)。
 */
@Service
@Transactional
public class FinInvoiceFacadeImpl implements FinInvoiceFacade {

    private final FinSalesInvoiceRepository salesInvoiceRepo;
    private final FinPurchaseInvoiceRepository purchaseInvoiceRepo;

    public FinInvoiceFacadeImpl(FinSalesInvoiceRepository salesInvoiceRepo,
                                FinPurchaseInvoiceRepository purchaseInvoiceRepo) {
        this.salesInvoiceRepo = salesInvoiceRepo;
        this.purchaseInvoiceRepo = purchaseInvoiceRepo;
    }

    @Override
    public Long createSalesInvoice(String sourceType, Long sourceId, String sourceNo,
                                   Long customerId, String customerName,
                                   BigDecimal total, LocalDate postingDate, LocalDate dueDate) {
        if (total == null || total.signum() < 0) {
            throw new IllegalArgumentException("发票金额不能为负: total=" + total);
        }
        FinSalesInvoice inv = new FinSalesInvoice();
        inv.setNo("SI-" + System.nanoTime() + "-" + (sourceId == null ? "X" : sourceId));
        inv.setPostingDate(postingDate == null ? LocalDate.now() : postingDate);
        inv.setDueDate(dueDate == null ? LocalDate.now().plusDays(30) : dueDate);
        inv.setCustomerId(customerId);
        inv.setCustomerName(customerName);
        inv.setStatus(InvoiceStatus.DRAFT.code);
        inv.setTotal(total);
        inv.setGrandTotal(total);
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setOutstandingAmount(total);
        inv.setSourceType(sourceType);
        inv.setSourceId(sourceId);
        inv.setSourceNo(sourceNo);
        inv.setRemark("来自 " + sourceType + "/" + sourceNo);
        return salesInvoiceRepo.save(inv).getId();
    }

    @Override
    public Long createPurchaseInvoice(String sourceType, Long sourceId, String sourceNo,
                                      Long supplierId, String supplierName,
                                      BigDecimal total, LocalDate postingDate, LocalDate dueDate) {
        if (total == null || total.signum() < 0) {
            throw new IllegalArgumentException("发票金额不能为负: total=" + total);
        }
        FinPurchaseInvoice inv = new FinPurchaseInvoice();
        inv.setNo("PI-" + System.nanoTime() + "-" + (sourceId == null ? "X" : sourceId));
        inv.setPostingDate(postingDate == null ? LocalDate.now() : postingDate);
        inv.setDueDate(dueDate == null ? LocalDate.now().plusDays(30) : dueDate);
        inv.setSupplierId(supplierId);
        inv.setSupplierName(supplierName);
        inv.setStatus(InvoiceStatus.DRAFT.code);
        inv.setTotal(total);
        inv.setGrandTotal(total);
        inv.setPaidAmount(BigDecimal.ZERO);
        inv.setOutstandingAmount(total);
        inv.setSourceType(sourceType);
        inv.setSourceId(sourceId);
        inv.setSourceNo(sourceNo);
        inv.setRemark("来自 " + sourceType + "/" + sourceNo);
        return purchaseInvoiceRepo.save(inv).getId();
    }
}
