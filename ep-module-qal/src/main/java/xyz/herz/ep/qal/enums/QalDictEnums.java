package xyz.herz.ep.qal.enums;

/**
 * 质量管理模块通用字典枚举(参考 ERPNext Quality Management DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.qal.core.QalEnumChoiceFetchHandler} 使用。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class QalDictEnums {
    private QalDictEnums() { }

    // ================= 启停状态(主数据通用:Criteria) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 质检单状态(参考 ERPNext Quality Inspection status) =================
    // 0草稿/1待检/2合格/3不合格/4已取消
    public enum InspectionStatus {
        DRAFT(0, "草稿"),
        PENDING(1, "待检"),
        PASSED(2, "合格"),
        REJECTED(3, "不合格"),
        CANCELLED(4, "已取消");
        public final int code;
        public final String label;
        InspectionStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 不合格品处理状态(参考 ERPNext Quality Non Conformance) =================
    // 0草稿/1已提交/2处理中/3已关闭/4已取消
    public enum NcStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        PROCESSING(2, "处理中"),
        CLOSED(3, "已关闭"),
        CANCELLED(4, "已取消");
        public final int code;
        public final String label;
        NcStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 不合格处置方式 =================
    // 1退货/2返工/3让步接收/4报废
    public enum NcDisposition {
        RETURN_GOODS(1, "退货"),
        REWORK(2, "返工"),
        CONCESSION(3, "让步接收"),
        SCRAP(4, "报废");
        public final int code;
        public final String label;
        NcDisposition(int c, String l) { this.code = c; this.label = l; }
    }
}
