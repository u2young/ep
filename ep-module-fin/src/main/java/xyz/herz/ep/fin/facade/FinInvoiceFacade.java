package xyz.herz.ep.fin.facade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 财务对外发票生成门面(由 erp/mall/crm 模块调用,触发应收/应付发票生成)。
 * <p>设计原则:
 * <ul>
 *   <li>调用方传入 partyId+partyName 快照(不 REF 跨模块实体,保持 fin 模块独立)</li>
 *   <li>生成后状态默认 DRAFT(0),由 fin 模块自身行按钮 Submit 触发 GL 凭证</li>
 *   <li>调用方失败时整体回滚(@Transactional REQUIRED)</li>
 * </ul>
 *
 * <p>典型用法(mall 模块订单支付后生成应收发票):
 * <pre>
 * &#64;Autowired(required = false) FinInvoiceFacade invoiceFacade;
 * if (invoiceFacade != null) {
 *     invoiceFacade.createSalesInvoice(
 *         "MallTradeOrder", order.getId(), order.getNo(),
 *         order.getCustomerId(), order.getCustomerName(),
 *         order.getPayAmount(), LocalDate.now(), LocalDate.now().plusDays(30));
 * }
 * </pre>
 */
public interface FinInvoiceFacade {

    /**
     * 生成销售(应收)发票。
     * @param sourceType 来源单据类型(如 "ErpSaleOut"/"MallTradeOrder"/"CrmContract")
     * @param sourceId 来源单据 id
     * @param sourceNo 来源单据号(快照)
     * @param customerId 客户 id(跨模块快照,不 REF)
     * @param customerName 客户名(快照)
     * @param total 发票金额
     * @param postingDate 入账日期
     * @param dueDate 到期日
     * @return FinSalesInvoice.id
     */
    Long createSalesInvoice(String sourceType, Long sourceId, String sourceNo,
                            Long customerId, String customerName,
                            BigDecimal total, LocalDate postingDate, LocalDate dueDate);

    /**
     * 生成采购(应付)发票。
     * @param sourceType 来源单据类型(如 "ErpPurchaseIn")
     * @param sourceId 来源单据 id
     * @param sourceNo 来源单据号(快照)
     * @param supplierId 供应商 id(跨模块快照,不 REF)
     * @param supplierName 供应商名(快照)
     * @param total 发票金额
     * @param postingDate 入账日期
     * @param dueDate 到期日
     * @return FinPurchaseInvoice.id
     */
    Long createPurchaseInvoice(String sourceType, Long sourceId, String sourceNo,
                               Long supplierId, String supplierName,
                               BigDecimal total, LocalDate postingDate, LocalDate dueDate);
}
