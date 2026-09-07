package xyz.herz.ep.sal.enums;

/**
 * 销售管理模块通用字典枚举(参考 ERPNext Selling DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler} 使用。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class SalDictEnums {
    private SalDictEnums() { }

    // ================= 启停状态(主数据通用:SalesPerson/SalesPartner) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 报价单状态(参考 ERPNext Quotation) =================
    // 0草稿/1已提交/2已取消/3已拒绝/4报价接受
    public enum QuotationStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消"),
        REJECTED(3, "已拒绝"),
        ACCEPTED(4, "已接受");
        public final int code;
        public final String label;
        QuotationStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 销售订单状态(参考 ERPNext Sales Order) =================
    // 0草稿/1已提交/2已暂停/3已取消/4已完成
    public enum SalesOrderStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        ON_HOLD(2, "已暂停"),
        CANCELLED(3, "已取消"),
        COMPLETED(4, "已完成");
        public final int code;
        public final String label;
        SalesOrderStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 送货单状态(参考 ERPNext Delivery Note) =================
    // 0草稿/1已提交/2已暂停/3已取消/4已发货
    public enum DeliveryNoteStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        ON_HOLD(2, "已暂停"),
        CANCELLED(3, "已取消"),
        DELIVERED(4, "已发货");
        public final int code;
        public final String label;
        DeliveryNoteStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 订单类型 =================
    public enum OrderType {
        SALES(1, "销售"),
        EXPENSE(2, "费用");
        public final int code;
        public final String label;
        OrderType(int c, String l) { this.code = c; this.label = l; }
    }
}
