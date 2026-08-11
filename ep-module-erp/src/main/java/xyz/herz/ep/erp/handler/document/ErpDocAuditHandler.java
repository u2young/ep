package xyz.herz.ep.erp.handler.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.common.inventory.InventoryChangeFacade;
import xyz.herz.ep.erp.core.ErpStockService;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseIn;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseInItem;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.sale.ErpSaleOut;
import xyz.herz.ep.erp.entity.sale.ErpSaleOutItem;
import xyz.herz.ep.erp.entity.sale.ErpSaleOrder;
import xyz.herz.ep.erp.enums.ErpDictEnums.AuditStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockBizType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ERP 单据「审核 / 反审核 / 关闭 / 作废」通用行按钮处理器。
 * <p>通过 {@code @RowOperation(operationHandler = ErpDocAuditHandler.class,
 *   operationParam = { ErpDocAuditHandler.CODE_APPROVE })} 绑定,
 * erupt 调用本 OperationHandler 的 exec(),取 eruptParams[0] 作为动作 code。
 */
@Component
public class ErpDocAuditHandler implements OperationHandler<Object, Object> {

    public static final String CODE_APPROVE   = "erp.doc.approve";
    public static final String CODE_UNAPPROVE = "erp.doc.unapprove";
    public static final String CODE_CLOSE     = "erp.doc.close";
    public static final String CODE_VOID      = "erp.doc.void";

    @PersistenceContext private EntityManager em;
    private final ErpStockService stockService;

    public ErpDocAuditHandler(ErpStockService stockService) { this.stockService = stockService; }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_APPROVE;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                switch (code) {
                    case CODE_APPROVE   -> approve(row);
                    case CODE_UNAPPROVE -> unapprove(row);
                    case CODE_CLOSE     -> transitionOnly(row, AuditStatus.CLOSED);
                    case CODE_VOID      -> transitionOnly(row, AuditStatus.VOIDED);
                    default -> throw new IllegalArgumentException("未知 code: " + code);
                }
                em.merge(row);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(label(row)).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    // =============== 分派 ===============

    private void approve(Object row) {
        if (row instanceof ErpPurchaseIn pi) {
            requireStatus(pi.getStatus(), AuditStatus.DRAFT);
            changeStock(pi);
            // 回写采购订单(若关联)的 inCount
            if (pi.getOrder() != null) backOrderInCountFromPurchaseIn(pi);
            pi.setStatus(AuditStatus.APPROVED.code);
        } else if (row instanceof ErpSaleOut so) {
            requireStatus(so.getStatus(), AuditStatus.DRAFT);
            changeStock(so);
            if (so.getOrder() != null) backOrderOutCountFromSaleOut(so);
            so.setStatus(AuditStatus.APPROVED.code);
        } else if (row instanceof ErpPurchaseOrder po) {
            requireStatus(po.getStatus(), AuditStatus.DRAFT, AuditStatus.CLOSED, AuditStatus.VOIDED);
            // 仅当 DRAFT(0) 才能审核
            if (AuditStatus.DRAFT.code != po.getStatus()) throw bad(po.getStatus(), "审核");
            po.setStatus(AuditStatus.APPROVED.code);
        } else if (row instanceof ErpSaleOrder so) {
            requireStatus(so.getStatus(), AuditStatus.DRAFT, AuditStatus.CLOSED, AuditStatus.VOIDED);
            if (AuditStatus.DRAFT.code != so.getStatus()) throw bad(so.getStatus(), "审核");
            so.setStatus(AuditStatus.APPROVED.code);
        } else {
            // 未来扩展的单据(其他出入库/调拨/盘点/收付款),只做状态迁移
            setStatus(row, AuditStatus.APPROVED.code);
        }
    }

    private void unapprove(Object row) {
        if (row instanceof ErpPurchaseIn pi) {
            requireStatus(pi.getStatus(), AuditStatus.APPROVED);
            reverseStock(pi);
            if (pi.getOrder() != null) rollbackOrderInCountFromPurchaseIn(pi);
            pi.setStatus(AuditStatus.DRAFT.code);
        } else if (row instanceof ErpSaleOut so) {
            requireStatus(so.getStatus(), AuditStatus.APPROVED);
            reverseStock(so);
            if (so.getOrder() != null) rollbackOrderOutCountFromSaleOut(so);
            so.setStatus(AuditStatus.DRAFT.code);
        } else if (row instanceof ErpPurchaseOrder po) {
            requireStatus(po.getStatus(), AuditStatus.APPROVED);
            // 只要订单还有已入库,就不能反审(避免入库单仍旧是已审核状态)
            if (nonZero(po.getInCount()) || nonZero(po.getReturnCount())) {
                throw new IllegalStateException("该采购订单已产生入库/退货,请先冲销下游入库/退货单再反审。");
            }
            po.setStatus(AuditStatus.DRAFT.code);
        } else if (row instanceof ErpSaleOrder so) {
            requireStatus(so.getStatus(), AuditStatus.APPROVED);
            if (nonZero(so.getOutCount()) || nonZero(so.getReturnCount())) {
                throw new IllegalStateException("该销售订单已产生出库/退货,请先冲销下游单据再反审。");
            }
            so.setStatus(AuditStatus.DRAFT.code);
        } else {
            setStatus(row, AuditStatus.DRAFT.code);
        }
    }

    /** 关闭/作废 —— 纯状态。 */
    private void transitionOnly(Object row, AuditStatus target) {
        Integer cur = readStatus(row);
        if (cur == null) cur = AuditStatus.DRAFT.code;
        if (target == AuditStatus.VOIDED && !cur.equals(AuditStatus.DRAFT.code)) {
            throw new IllegalStateException("只有草稿可以作废,当前状态=" + label(cur));
        }
        if (target == AuditStatus.CLOSED && (cur.equals(AuditStatus.VOIDED.code) || cur.equals(AuditStatus.CLOSED.code))) {
            throw new IllegalStateException("已终止状态不能再关闭");
        }
        setStatus(row, target.code);
    }

    // =============== 库存变更封装 ===============

    private void changeStock(ErpPurchaseIn pi) {
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            pi.getNo(), pi.getId(), StockBizType.PURCHASE_IN.code, List.of(),
            "采购入库审核" + (pi.getRemark() == null ? "" : ": " + pi.getRemark()));
        List<ErpStockService.DetailedItem> details = new ArrayList<>();
        for (ErpPurchaseInItem it : pi.getItems()) {
            BigDecimal qty = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            if (qty.signum() == 0) continue;
            details.add(new ErpStockService.DetailedItem(
                it.getProduct().getId(),
                it.getSku() == null ? null : it.getSku().getId(),
                pi.getWarehouse().getId(),
                it.getBatchNo(),
                qty,
                it.getProductPrice()
            ));
        }
        stockService.changeDetailed(req, details);
    }

    private void reverseStock(ErpPurchaseIn pi) {
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            pi.getNo(), pi.getId() + 10_000_000_000L /* 伪造一个新 bizId 做反向流水,真实系统建议用单独流水 */,
            StockBizType.PURCHASE_RETURN_OUT.code, List.of(),
            "采购入库反审冲销");
        List<ErpStockService.DetailedItem> details = new ArrayList<>();
        for (ErpPurchaseInItem it : pi.getItems()) {
            BigDecimal qty = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            if (qty.signum() == 0) continue;
            details.add(new ErpStockService.DetailedItem(
                it.getProduct().getId(),
                it.getSku() == null ? null : it.getSku().getId(),
                pi.getWarehouse().getId(),
                it.getBatchNo(),
                qty.negate(),
                null));
        }
        stockService.changeDetailed(req, details);
    }

    private void changeStock(ErpSaleOut so) {
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            so.getNo(), so.getId(), StockBizType.SALE_OUT.code, List.of(),
            "销售出库审核" + (so.getRemark() == null ? "" : ": " + so.getRemark()));
        List<ErpStockService.DetailedItem> details = new ArrayList<>();
        for (ErpSaleOutItem it : so.getItems()) {
            BigDecimal qty = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            if (qty.signum() == 0) continue;
            details.add(new ErpStockService.DetailedItem(
                it.getProduct().getId(),
                it.getSku() == null ? null : it.getSku().getId(),
                so.getWarehouse().getId(),
                it.getBatchNo(),
                qty.negate(),  // 出库方向为负
                it.getProductPrice()
            ));
        }
        stockService.changeDetailed(req, details);
    }

    private void reverseStock(ErpSaleOut so) {
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            so.getNo(), so.getId() + 10_000_000_000L,
            StockBizType.SALE_RETURN_IN.code, List.of(),
            "销售出库反审冲销");
        List<ErpStockService.DetailedItem> details = new ArrayList<>();
        for (ErpSaleOutItem it : so.getItems()) {
            BigDecimal qty = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            if (qty.signum() == 0) continue;
            details.add(new ErpStockService.DetailedItem(
                it.getProduct().getId(),
                it.getSku() == null ? null : it.getSku().getId(),
                so.getWarehouse().getId(),
                it.getBatchNo(),
                qty, null));
        }
        stockService.changeDetailed(req, details);
    }

    // =============== 回写订单 in_count/out_count(P0 简化:行级和总级同时加) ===============

    private void backOrderInCountFromPurchaseIn(ErpPurchaseIn pi) {
        ErpPurchaseOrder po = em.find(ErpPurchaseOrder.class, pi.getOrder().getId());
        if (po == null) return;
        BigDecimal totalIn = BigDecimal.ZERO;
        for (ErpPurchaseInItem it : pi.getItems()) {
            BigDecimal c = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            totalIn = totalIn.add(c);
            if (it.getOrderItemId() != null) {
                var row = em.find(xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem.class, it.getOrderItemId());
                if (row != null) {
                    row.setInCount(row.getInCount() == null ? c : row.getInCount().add(c));
                }
            }
        }
        po.setInCount(po.getInCount() == null ? totalIn : po.getInCount().add(totalIn));
    }

    private void rollbackOrderInCountFromPurchaseIn(ErpPurchaseIn pi) {
        ErpPurchaseOrder po = em.find(ErpPurchaseOrder.class, pi.getOrder().getId());
        if (po == null) return;
        BigDecimal totalIn = BigDecimal.ZERO;
        for (ErpPurchaseInItem it : pi.getItems()) {
            BigDecimal c = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            totalIn = totalIn.add(c);
            if (it.getOrderItemId() != null) {
                var row = em.find(xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem.class, it.getOrderItemId());
                if (row != null) {
                    BigDecimal left = (row.getInCount() == null ? BigDecimal.ZERO : row.getInCount()).subtract(c);
                    row.setInCount(left.max(BigDecimal.ZERO));
                }
            }
        }
        BigDecimal left = (po.getInCount() == null ? BigDecimal.ZERO : po.getInCount()).subtract(totalIn);
        po.setInCount(left.max(BigDecimal.ZERO));
    }

    private void backOrderOutCountFromSaleOut(ErpSaleOut so) {
        ErpSaleOrder oo = em.find(ErpSaleOrder.class, so.getOrder().getId());
        if (oo == null) return;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (ErpSaleOutItem it : so.getItems()) {
            BigDecimal c = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            totalOut = totalOut.add(c);
            if (it.getOrderItemId() != null) {
                var row = em.find(xyz.herz.ep.erp.entity.sale.ErpSaleOrderItem.class, it.getOrderItemId());
                if (row != null) {
                    row.setOutCount(row.getOutCount() == null ? c : row.getOutCount().add(c));
                }
            }
        }
        oo.setOutCount(oo.getOutCount() == null ? totalOut : oo.getOutCount().add(totalOut));
    }

    private void rollbackOrderOutCountFromSaleOut(ErpSaleOut so) {
        ErpSaleOrder oo = em.find(ErpSaleOrder.class, so.getOrder().getId());
        if (oo == null) return;
        BigDecimal totalOut = BigDecimal.ZERO;
        for (ErpSaleOutItem it : so.getItems()) {
            BigDecimal c = it.getCount() == null ? BigDecimal.ZERO : it.getCount();
            totalOut = totalOut.add(c);
            if (it.getOrderItemId() != null) {
                var row = em.find(xyz.herz.ep.erp.entity.sale.ErpSaleOrderItem.class, it.getOrderItemId());
                if (row != null) {
                    BigDecimal left = (row.getOutCount() == null ? BigDecimal.ZERO : row.getOutCount()).subtract(c);
                    row.setOutCount(left.max(BigDecimal.ZERO));
                }
            }
        }
        BigDecimal left = (oo.getOutCount() == null ? BigDecimal.ZERO : oo.getOutCount()).subtract(totalOut);
        oo.setOutCount(left.max(BigDecimal.ZERO));
    }

    // =============== 状态/反射辅助 ===============

    private void requireStatus(Integer current, AuditStatus... allowed) {
        if (current == null) current = AuditStatus.DRAFT.code;
        for (AuditStatus a : allowed) if (a.code == current) return;
        throw new IllegalStateException("当前状态不允许此操作: state=" + label(current));
    }

    private IllegalStateException bad(int cur, String op) {
        return new IllegalStateException("当前状态 " + label(cur) + " 无法执行「" + op + "」");
    }

    private static String label(int code) {
        for (AuditStatus a : AuditStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }

    private String label(Object row) {
        try {
            java.lang.reflect.Method gNo = findGetter(row.getClass(), "getNo");
            Object no = gNo.invoke(row);
            if (no != null) return row.getClass().getSimpleName() + "#" + no;
        } catch (Exception ignore) {}
        return row.getClass().getSimpleName();
    }

    private static boolean nonZero(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    private static Integer readStatus(Object row) {
        try { return (Integer) findGetter(row.getClass(), "getStatus").invoke(row); }
        catch (Exception e) { throw new IllegalStateException("缺少 getStatus(): " + row.getClass(), e); }
    }

    private static void setStatus(Object row, int v) {
        try {
            java.lang.reflect.Method s = row.getClass().getMethod("setStatus", Integer.class);
            s.invoke(row, v);
        } catch (NoSuchMethodException e1) {
            try {
                java.lang.reflect.Method s = row.getClass().getMethod("setStatus", int.class);
                s.invoke(row, v);
            } catch (Exception e2) {
                throw new IllegalStateException("缺少 setStatus: " + row.getClass(), e2);
            }
        } catch (Exception e) {
            throw new IllegalStateException("setStatus 失败", e);
        }
    }

    private static java.lang.reflect.Method findGetter(Class<?> c, String name) throws NoSuchMethodException {
        Class<?> cur = c;
        while (cur != null) {
            try { java.lang.reflect.Method m = cur.getDeclaredMethod(name); m.setAccessible(true); return m; }
            catch (NoSuchMethodException ignore) { cur = cur.getSuperclass(); }
        }
        throw new NoSuchMethodException(c.getName() + "#" + name);
    }
}
