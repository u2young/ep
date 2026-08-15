package xyz.herz.ep.erp.handler.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade;
import xyz.herz.ep.erp.core.ErpStockService;
import xyz.herz.ep.erp.entity.document.ErpStockOut;
import xyz.herz.ep.erp.entity.document.ErpStockOutItem;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockDocStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockOutBizType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 其他出库单「审核 / 反审」行按钮处理器。
 * <p>审核(0→1):对每个 item 调用库存变更(出库-,bizType 由 StockOutBizType 映射)。
 * <p>反审(1→0):对每个 item 反向冲销(入库+)。
 */
@Component
public class ErpStockOutAuditHandler implements OperationHandler<Object, Object> {

    public static final String CODE_AUDIT   = "erp.stockout.audit";
    public static final String CODE_UNAUDIT = "erp.stockout.unaudit";

    /** 反审时伪造一个反向 bizId,避免与审核流水幂等键冲突。 */
    private static final long REVERSE_BIZ_ID_OFFSET = 10_000_000_000L;

    @PersistenceContext private EntityManager em;
    private final ErpStockService stockService;

    public ErpStockOutAuditHandler(ErpStockService stockService) {
        this.stockService = stockService;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_AUDIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof ErpStockOut doc)) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": 仅支持其他出库单; ");
                continue;
            }
            try {
                switch (code) {
                    case CODE_AUDIT   -> audit(doc);
                    case CODE_UNAUDIT -> unaudit(doc);
                    default -> throw new IllegalArgumentException("未知 code: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(label(doc)).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    // =============== 审核 ===============

    private void audit(ErpStockOut doc) {
        if (doc.getStatus() == null || doc.getStatus() != StockDocStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可以审核,当前状态=" + label(doc.getStatus()));
        }
        StockOutBizType biz = StockOutBizType.of(doc.getBizType());
        // 审核出库:qty 取负(出库方向)
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            doc.getNo(), doc.getId(), biz.toStockBizTypeCode(), List.of(),
            "其他出库审核" + (doc.getRemark() == null ? "" : ": " + doc.getRemark()));
        List<ErpStockService.DetailedItem> details = buildDetails(doc, true);
        stockService.changeDetailed(req, details);
        doc.setStatus(StockDocStatus.APPROVED.code);
    }

    private void unaudit(ErpStockOut doc) {
        if (doc.getStatus() == null || doc.getStatus() != StockDocStatus.APPROVED.code) {
            throw new IllegalStateException("只有已审核状态可以反审,当前状态=" + label(doc.getStatus()));
        }
        StockOutBizType biz = StockOutBizType.of(doc.getBizType());
        // 反向冲销:bizId 加偏移避免幂等键冲突;bizType 用反向(入库)code;数量取正(入库方向)。
        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            doc.getNo(), doc.getId() + REVERSE_BIZ_ID_OFFSET,
            biz.toReverseStockBizTypeCode(), List.of(), "其他出库反审冲销");
        List<ErpStockService.DetailedItem> details = buildDetails(doc, false);
        stockService.changeDetailed(req, details);
        doc.setStatus(StockDocStatus.DRAFT.code);
    }

    /**
     * @param negate true=数量取负(出库方向),false=数量保持正(入库方向)
     */
    private List<ErpStockService.DetailedItem> buildDetails(ErpStockOut doc, boolean negate) {
        List<ErpStockService.DetailedItem> details = new ArrayList<>();
        for (ErpStockOutItem it : doc.getItems()) {
            int raw = it.getQty() == null ? 0 : it.getQty();
            if (raw == 0) continue;
            BigDecimal qty = BigDecimal.valueOf(raw);
            if (negate) qty = qty.negate();
            details.add(new ErpStockService.DetailedItem(
                it.getSku().getProduct().getId(),
                it.getSku().getId(),
                doc.getWarehouse().getId(),
                null,
                qty,
                null
            ));
        }
        return details;
    }

    private String label(ErpStockOut doc) {
        return ErpStockOut.class.getSimpleName() + "#" + doc.getNo();
    }

    private static String label(Integer code) {
        if (code == null) return "null";
        for (StockDocStatus s : StockDocStatus.values()) if (s.code == code) return s.label;
        return "UNKNOWN(" + code + ")";
    }
}
