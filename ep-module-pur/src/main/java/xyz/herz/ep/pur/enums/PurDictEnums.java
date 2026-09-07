package xyz.herz.ep.pur.enums;

/**
 * 采购管理模块通用字典枚举(参考 ERPNext Procurement DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.pur.core.PurEnumChoiceFetchHandler} 使用。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class PurDictEnums {
    private PurDictEnums() { }

    // ================= 启停状态(主数据通用:Settings 默认采购员) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 请购单状态(参考 ERPNext Material Request) =================
    // 0草稿/1已提交/2已转单/3已取消
    public enum RequisitionStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CONVERTED(2, "已转单"),
        CANCELLED(3, "已取消");
        public final int code;
        public final String label;
        RequisitionStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 询价单状态(参考 ERPNext Request for Quotation) =================
    // 0草稿/1已发送/2已收报价/3已取消
    public enum RfqStatus {
        DRAFT(0, "草稿"),
        SENT(1, "已发送"),
        RECEIVED(2, "已收报价"),
        CANCELLED(3, "已取消");
        public final int code;
        public final String label;
        RfqStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 采购收货单状态(参考 ERPNext Purchase Receipt) =================
    // 0草稿/1已提交/2已收货/3已取消
    public enum ReceiptStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        RECEIVED(2, "已收货"),
        CANCELLED(3, "已取消");
        public final int code;
        public final String label;
        ReceiptStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 询价优先级(参考 ERPNext priority) =================
    public enum Priority {
        LOW(1, "低"),
        MEDIUM(2, "中"),
        HIGH(3, "高"),
        URGENT(4, "紧急");
        public final int code;
        public final String label;
        Priority(int c, String l) { this.code = c; this.label = l; }
    }
}
