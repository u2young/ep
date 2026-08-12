package xyz.herz.ep.common.facade;

/**
 * Mall → ERP 库存操作 Facade。
 * Mall 模块通过此接口调用 ERP 的库存变更能力,
 * erupt-cloud 架构下可替换为 RPC 实现。
 */
public interface MallStockFacade {
    /**
     * 锁定库存(下单时)
     * @param skuCode SKU编码
     * @param warehouseId 仓库ID
     * @param qty 数量
     * @return true=成功
     */
    boolean lockStock(String skuCode, Long warehouseId, int qty);

    /**
     * 解锁库存(取消订单时)
     */
    boolean unlockStock(String skuCode, Long warehouseId, int qty);

    /**
     * 出库(发货时,扣减实物)
     */
    boolean deductStock(String skuCode, Long warehouseId, int qty);

    /**
     * 入库(售后退货时,增加实物)
     */
    boolean returnStock(String skuCode, Long warehouseId, int qty);
}
