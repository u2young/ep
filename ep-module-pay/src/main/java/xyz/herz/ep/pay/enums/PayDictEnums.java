package xyz.herz.ep.pay.enums;

/**
 * 薪酬管理模块通用字典枚举(参考 ERPNext Payroll DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.pay.core.PayEnumChoiceFetchHandler} 使用。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class PayDictEnums {
    private PayDictEnums() { }

    // ================= 启停状态(主数据通用:Component/Structure) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工资项类型(参考 ERPNext salary component type) =================
    // 1收入/2扣款
    public enum ComponentType {
        EARNING(1, "收入"),
        DEDUCTION(2, "扣款");
        public final int code;
        public final String label;
        ComponentType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工资单状态(参考 ERPNext Salary Slip status) =================
    // 0草稿/1已提交/2已过账/3已取消
    public enum SlipStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        POSTED(2, "已过账"),
        CANCELLED(3, "已取消");
        public final int code;
        public final String label;
        SlipStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
