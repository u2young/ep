package xyz.herz.ep.sup.enums;

/**
 * 服务支持模块通用字典枚举(参考 ERPNext Support DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.sup.core.SupEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "IssueStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿/打开(0)状态下表单直改。
 */
public final class SupDictEnums {
    private SupDictEnums() { }

    // ================= 启停状态(主数据通用:SLA/优先级) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工单状态(0打开/1已回复/2已解决/3已关闭/4重新打开/5取消) =================
    public enum IssueStatus {
        OPEN(0, "打开"),
        REPLIED(1, "已回复"),
        RESOLVED(2, "已解决"),
        CLOSED(3, "已关闭"),
        REOPENED(4, "重新打开"),
        CANCELLED(5, "已取消");
        public final int code;
        public final String label;
        IssueStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工单优先级(1低/2中/3高/4紧急) =================
    public enum IssuePriority {
        LOW(1, "低"),
        MEDIUM(2, "中"),
        HIGH(3, "高"),
        URGENT(4, "紧急");
        public final int code;
        public final String label;
        IssuePriority(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 知识库状态(0草稿/1已发布/2归档) =================
    public enum KbStatus {
        DRAFT(0, "草稿"),
        PUBLISHED(1, "已发布"),
        ARCHIVED(2, "已归档");
        public final int code;
        public final String label;
        KbStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
