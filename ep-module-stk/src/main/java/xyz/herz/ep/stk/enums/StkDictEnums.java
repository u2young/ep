package xyz.herz.ep.stk.enums;

/**
 * 库存管理模块通用字典枚举(参考 ERPNext Stock DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.stk.core.StkEnumChoiceFetchHandler} 使用。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class StkDictEnums {
    private StkDictEnums() { }

    // ================= 启停状态(主数据通用:Settings/SerialNo/Batch) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 库存出入库单状态(参考 ERPNext Stock Entry) =================
    // 0草稿/1已提交/2已取消
    public enum EntryStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        EntryStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 盘点单状态(参考 ERPNext Stock Reconciliation) =================
    // 0草稿/1已提交/2已取消
    public enum ReconciliationStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        ReconciliationStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 出入库类型(参考 ERPNext Stock Entry purpose) =================
    public enum EntryType {
        MATERIAL_RECEIPT(1, "入库"),
        MATERIAL_ISSUE(2, "出库"),
        MATERIAL_TRANSFER(3, "移库"),
        MANUFACTURE(4, "生产"),
        REPACK(5, "翻包");
        public final int code;
        public final String label;
        EntryType(int c, String l) { this.code = c; this.label = l; }
    }
}
