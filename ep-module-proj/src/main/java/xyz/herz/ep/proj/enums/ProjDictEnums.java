package xyz.herz.ep.proj.enums;

/**
 * 项目管理模块通用字典枚举(参考 ERPNext Projects DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.proj.core.ProjEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "ProjectStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class ProjDictEnums {
    private ProjDictEnums() { }

    // ================= 启停状态(主数据通用:项目模板) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 项目状态(0草稿/1立项/2进行/3暂停/4完工/5取消) =================
    public enum ProjectStatus {
        DRAFT(0, "草稿"),
        APPROVED(1, "已立项"),
        IN_PROGRESS(2, "进行中"),
        ON_HOLD(3, "已暂停"),
        COMPLETED(4, "已完工"),
        CANCELLED(5, "已取消");
        public final int code;
        public final String label;
        ProjectStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 任务状态(0未开始/1进行/2完成/3逾期/4取消) =================
    public enum TaskStatus {
        NOT_STARTED(0, "未开始"),
        IN_PROGRESS(1, "进行中"),
        COMPLETED(2, "已完成"),
        OVERDUE(3, "已逾期"),
        CANCELLED(4, "已取消");
        public final int code;
        public final String label;
        TaskStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 任务优先级(1低/2中/3高/4紧急) =================
    public enum TaskPriority {
        LOW(1, "低"),
        MEDIUM(2, "中"),
        HIGH(3, "高"),
        URGENT(4, "紧急");
        public final int code;
        public final String label;
        TaskPriority(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工时计费状态(0未计费/1已计费/2已开票) =================
    public enum BillingStatus {
        UNBILLED(0, "未计费"),
        BILLED(1, "已计费"),
        INVOICED(2, "已开票");
        public final int code;
        public final String label;
        BillingStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 费用报销状态(0草稿/1已提交/2已批准/3已拒绝/4已入账) =================
    public enum ExpenseClaimStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        APPROVED(2, "已批准"),
        REJECTED(3, "已拒绝"),
        POSTED(4, "已入账");
        public final int code;
        public final String label;
        ExpenseClaimStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 现金流类型(1流入/2流出) =================
    public enum CashFlowType {
        INFLOW(1, "流入"),
        OUTFLOW(2, "流出");
        public final int code;
        public final String label;
        CashFlowType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 活动成本类型(1人工/2费用/3物料) =================
    public enum ActivityCostType {
        LABOR(1, "人工"),
        EXPENSE(2, "费用"),
        MATERIAL(3, "物料");
        public final int code;
        public final String label;
        ActivityCostType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 任务树展开节点类型(供 proj_task 的 isGroup 标记) =================
    public enum TaskNodeType {
        LEAF(0, "叶子"),
        GROUP(1, "汇总");
        public final int code;
        public final String label;
        TaskNodeType(int c, String l) { this.code = c; this.label = l; }
    }
}
