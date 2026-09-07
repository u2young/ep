package xyz.herz.ep.mfg.handler.stockentry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntry;
import xyz.herz.ep.mfg.entity.stockentry.MfgStockEntryItem;
import xyz.herz.ep.mfg.enums.MfgDictEnums.StockEntryStatus;
import xyz.herz.ep.mfg.enums.MfgDictEnums.StockEntryType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产出入库单「审核」行按钮处理器(核心:库存联动)。
 * <p>状态迁移:DRAFT(0) → AUDITED(1)。
 * <p>审核时触发 {@link InventoryChangeFacade#change} 联动 ERP 库存:
 * <ul>
 *   <li>领料(MATERIAL_ISSUE):从 fromWarehouse 出库,qty 为负</li>
 *   <li>完工入库(COMPLETION_IN):入 toWarehouse,qty 为正</li>
 *   <li>退料(MATERIAL_RETURN):入 toWarehouse,qty 为正</li>
 *   <li>委外发料(SUBCONTRACT_ISSUE):从 fromWarehouse 出库,qty 为负</li>
 *   <li>委外收货(SUBCONTRACT_RECEIPT):入 toWarehouse,qty 为正</li>
 * </ul>
 * 幂等键:(stockBizType, stockEntry.id),重复审核不会重复扣库存。
 */
@Component
public class MfgStockEntryAuditHandler implements OperationHandler<Object, Object> {

    public static final String CODE_AUDIT = "mfg.stockentry.audit";

    @PersistenceContext private EntityManager em;

    /** 跨模块可选注入:ep-boot 全量启动时由 ErpStockService 提供;模块独立测试需扫 erp 包。 */
    @Autowired(required = false)
    private InventoryChangeFacade inventoryFacade;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof MfgStockEntry doc)) {
                fail++; sb.append("仅支持出入库单; "); continue;
            }
            try {
                auditOne(doc);
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("单据#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_AUDIT);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void auditOne(MfgStockEntry doc) {
        Integer st = doc.getStatus();
        if (st == null || st != StockEntryStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可以审核,当前状态=" + st);
        }
        if (doc.getItems() == null || doc.getItems().isEmpty()) {
            throw new IllegalStateException("明细为空,无法审核");
        }
        if (inventoryFacade == null) {
            throw new IllegalStateException("InventoryChangeFacade 未注入,无法审核出入库单(请确认 ep-module-erp 已启用)");
        }

        int bizTypeCode = StockEntryType.of(doc.getTtype()).stockBizTypeCode();
        boolean inbound = isInbound(doc.getTtype());
        var wh = inbound ? doc.getToWarehouse() : doc.getFromWarehouse();
        if (wh == null || wh.getId() == null) {
            throw new IllegalStateException((inbound ? "目标仓" : "来源仓") + " 不能为空");
        }
        Long warehouseId = wh.getId();

        List<InventoryChangeFacade.ChangeItem> items = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (MfgStockEntryItem it : doc.getItems()) {
            if (it.getProduct() == null || it.getProduct().getId() == null) {
                throw new IllegalStateException("明细行产品不能为空");
            }
            BigDecimal qty = it.getQty() == null ? BigDecimal.ZERO : it.getQty();
            if (qty.signum() <= 0) {
                throw new IllegalStateException("明细数量必须 > 0");
            }
            // 入库正数,出库负数(InventoryChangeFacade 方向校验)
            BigDecimal signedQty = inbound ? qty : qty.negate();
            items.add(new InventoryChangeFacade.ChangeItem(
                it.getProduct().getId(), warehouseId, signedQty));
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(it.getAmount() == null ? BigDecimal.ZERO : it.getAmount());
        }

        InventoryChangeFacade.ChangeRequest req = new InventoryChangeFacade.ChangeRequest(
            doc.getNo(), doc.getId(), bizTypeCode, items,
            doc.getRemark() == null ? "生产出入库审核" : doc.getRemark());
        inventoryFacade.change(req);

        doc.setTotalQty(totalQty);
        doc.setTotalAmount(totalAmount);
        doc.setStatus(StockEntryStatus.AUDITED.code);
    }

    private boolean isInbound(Integer ttype) {
        return ttype != null && (ttype == StockEntryType.COMPLETION_IN.code
            || ttype == StockEntryType.MATERIAL_RETURN.code
            || ttype == StockEntryType.SUBCONTRACT_RECEIPT.code);
    }
}
