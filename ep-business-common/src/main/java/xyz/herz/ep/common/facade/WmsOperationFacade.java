package xyz.herz.ep.common.facade;

/**
 * ERP → WMS 仓储操作 Facade。
 * ERP 审核出入库单时可调用 WMS 的库位级操作。
 */
public interface WmsOperationFacade {
    /**
     * 创建入库ASN(ERP采购入库审核时)
     */
    Long createAsn(Long warehouseId, String supplierName, java.util.List<AsnItem> items);

    /**
     * 创建出库通知(ERP销售出库审核时)
     */
    Long createShipmentNotice(Long warehouseId, String customerName, java.util.List<ShipmentItem> items);

    record AsnItem(String skuCode, String skuName, int expectedQty) {}
    record ShipmentItem(String skuCode, String skuName, int expectedQty) {}
}
