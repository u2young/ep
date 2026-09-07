package xyz.herz.ep.pur.handler.requisition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.pur.entity.requisition.PurPurchaseRequisition;
import xyz.herz.ep.pur.entity.requisition.PurRequisitionItem;
import xyz.herz.ep.pur.enums.PurDictEnums.RequisitionStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 请购单生命周期行按钮处理器(参考 ERPNext Material Request 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细存在 + 回写 totalAmount 明细合计)
 * / 转单(SUBMITTED→CONVERTED,请购单转为采购订单/询价单的终态标记)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,已转单终态不可取消)。
 *
 * <p>转单语义:本模块只负责把请购单置为「已转单」终态;实际创建询价单/采购订单
 * 由外部模块(erp/询价单)通过 sourceRequisitionNo 快照引用,避免跨模块硬依赖。
 */
@Component
public class PurRequisitionLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "pur.req.submit";
    public static final String CODE_CONVERT = "pur.req.convert";
    public static final String CODE_CANCEL = "pur.req.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof PurPurchaseRequisition doc)) {
                fail++; sb.append("仅支持请购单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_CONVERT -> applyConvert(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("请购单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细存在 + 回写 totalAmount = Σ(item.amount)。 */
    private void applySubmit(PurPurchaseRequisition doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != RequisitionStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        List<PurRequisitionItem> items = doc.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("请购明细不能为空,无法提交");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurRequisitionItem it : items) {
            BigDecimal amount = it.getAmount();
            if (amount == null) {
                // amount = qty × estimatedPrice,缺省按 0 处理
                BigDecimal q = it.getQty() == null ? BigDecimal.ZERO : it.getQty();
                BigDecimal p = it.getEstimatedPrice() == null ? BigDecimal.ZERO : it.getEstimatedPrice();
                amount = q.multiply(p);
                it.setAmount(amount);
            }
            total = total.add(amount);
        }
        doc.setTotalAmount(total);
        doc.setStatus(RequisitionStatus.SUBMITTED.code);
    }

    /** 转单:已提交 → 已转单(终态);外部模块已通过 sourceRequisitionNo 引用本单。 */
    private void applyConvert(PurPurchaseRequisition doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != RequisitionStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可转单,当前状态码: " + st);
        }
        doc.setStatus(RequisitionStatus.CONVERTED.code);
    }

    /** 取消:草稿/已提交 → 已取消;已转单终态不可取消(已生成下游单据)。 */
    private void applyCancel(PurPurchaseRequisition doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == RequisitionStatus.CONVERTED.code) {
            throw new IllegalStateException("已转单不可取消(下游单据已生成),当前状态码: " + st);
        }
        if (st == RequisitionStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(RequisitionStatus.CANCELLED.code);
    }
}
