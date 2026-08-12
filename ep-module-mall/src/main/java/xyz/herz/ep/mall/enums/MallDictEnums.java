package xyz.herz.ep.mall.enums;

/**
 * 商城通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "OrderStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都有 int code + String label,状态值跳跃式递增以预留扩展位。
 */
public final class MallDictEnums {
    private MallDictEnums() { }

    // ================= 订单状态机 =================
    public enum OrderStatus {
        UNPAID(0, "待付款"),
        UNDELIVERED(10, "待发货"),
        UNRECEIVED(20, "待收货"),
        COMPLETED(30, "已完成"),
        CANCELED(40, "已取消");
        public final int code;
        public final String label;
        OrderStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 售后状态机 =================
    public enum AfterSaleStatus {
        APPLY(10, "申请"),
        AGREED(20, "同意"),
        BUYER_DELIVERY(30, "买家发货"),
        SELLER_RECEIVE(40, "卖家收货"),
        WAIT_REFUND(50, "待退款"),
        SUCCESS(60, "完成"),
        REJECTED(70, "拒绝");
        public final int code;
        public final String label;
        AfterSaleStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 售后类型 =================
    public enum AfterSaleType {
        REFUND_ONLY(1, "仅退款"),
        RETURN_REFUND(2, "退货退款"),
        EXCHANGE(3, "换货");
        public final int code;
        public final String label;
        AfterSaleType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 支付状态 =================
    public enum PayStatus {
        UNPAID(0, "待支付"),
        PAID(10, "已支付"),
        REFUNDED(20, "已退款");
        public final int code;
        public final String label;
        PayStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 启停状态(主数据通用) =================
    public enum EnableStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 商品上下架状态 =================
    public enum ListingStatus {
        DELISTED(0, "下架"),
        LISTED(1, "上架");
        public final int code;
        public final String label;
        ListingStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
