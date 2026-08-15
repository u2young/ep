package xyz.herz.ep.erp.inventory;

/** 库存不足:出库数量 > (可用余额 - 预扣)。 */
public class InventoryShortageException extends RuntimeException {
    public final Long productId;
    public final Long warehouseId;
    public final java.math.BigDecimal currentQty;
    public final java.math.BigDecimal requireQty;

    public InventoryShortageException(Long productId, Long warehouseId,
                                      java.math.BigDecimal currentQty,
                                      java.math.BigDecimal requireQty) {
        super(String.format(
                "库存不足:产品[%s] 仓库[%s] 当前%.4f, 需要%.4f",
                productId, warehouseId, currentQty, requireQty));
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.currentQty = currentQty;
        this.requireQty = requireQty;
    }
}
