package xyz.herz.ep.wms.enums;

/**
 * WMS 通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "AsnStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 */
public final class WmsDictEnums {
    private WmsDictEnums() { }

    // ================= ASN 入库通知状态 =================
    public enum AsnStatus {
        NEW(0, "新建"),
        PARTIAL_RECEIVED(10, "部分收货"),
        RECEIVED(20, "已收货"),
        CLOSED(30, "关闭");
        public final int code;
        public final String label;
        AsnStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 收货单状态 =================
    public enum ReceiptStatus {
        NEW(0, "新建"),
        RECEIVING(10, "收货中"),
        COMPLETED(20, "已完成");
        public final int code;
        public final String label;
        ReceiptStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 上架单状态 =================
    public enum PutawayStatus {
        NEW(0, "新建"),
        PUTTING(10, "上架中"),
        COMPLETED(20, "已完成");
        public final int code;
        public final String label;
        PutawayStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 出库通知状态 =================
    public enum ShipmentNoticeStatus {
        NEW(0, "新建"),
        PARTIAL_PICKED(10, "部分拣货"),
        PICKED(20, "已拣货"),
        CLOSED(30, "关闭");
        public final int code;
        public final String label;
        ShipmentNoticeStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 拣货单状态 =================
    public enum PickStatus {
        NEW(0, "新建"),
        PICKING(10, "拣货中"),
        COMPLETED(20, "已完成");
        public final int code;
        public final String label;
        PickStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 库位状态 =================
    public enum LocationStatus {
        IDLE(0, "空闲"),
        HAS_STOCK(1, "有货"),
        LOCKED(2, "锁定"),
        COUNTING(3, "盘点中");
        public final int code;
        public final String label;
        LocationStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 移库作业单状态 =================
    public enum StockMoveOrderStatus {
        NEW(0, "新建"),
        MOVING(10, "移库中"),
        COMPLETED(20, "已完成");
        public final int code;
        public final String label;
        StockMoveOrderStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 盘点单状态 =================
    public enum StockCheckStatus {
        NEW(0, "新建"),
        CHECKING(10, "盘点中"),
        COMPLETED(20, "已完成"),
        HAS_DIFF(30, "有差异");
        public final int code;
        public final String label;
        StockCheckStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 启停状态(主数据通用) =================
    public enum EnableStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
