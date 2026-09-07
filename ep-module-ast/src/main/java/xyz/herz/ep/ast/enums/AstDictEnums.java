package xyz.herz.ep.ast.enums;

/**
 * 资产管理模块通用字典枚举(参考 ERPNext Asset DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.ast.core.AstEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "AssetStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class AstDictEnums {
    private AstDictEnums() { }

    // ================= 启停状态(主数据通用:Category/Location) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 资产状态(参考 ERPNext Asset status) =================
    // 0草稿/1可用(已提交)/2部分折旧/3完全折旧/4出售/5报废
    public enum AssetStatus {
        DRAFT(0, "草稿"),
        AVAILABLE(1, "可用"),
        PARTIALLY_DEPRECIATED(2, "部分折旧"),
        FULLY_DEPRECIATED(3, "完全折旧"),
        SOLD(4, "出售"),
        SCRAPPED(5, "报废");
        public final int code;
        public final String label;
        AssetStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 折旧方法(参考 ERPNext depreciation_method) =================
    // 0手工(不折旧)/1直线法/2余额递减法/3双倍余额递减法
    public enum DepreciationMethod {
        MANUAL(0, "手工"),
        STRAIGHT_LINE(1, "直线法"),
        DECLINING_BALANCE(2, "余额递减法"),
        DOUBLE_DECLINING(3, "双倍余额递减法");
        public final int code;
        public final String label;
        DepreciationMethod(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 资产移动类型 =================
    // 1位置变更/2保管人变更/3部门变更/4成本中心变更
    public enum MovementType {
        LOCATION(1, "位置变更"),
        CUSTODIAN(2, "保管人变更"),
        DEPARTMENT(3, "部门变更"),
        COST_CENTER(4, "成本中心变更");
        public final int code;
        public final String label;
        MovementType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 移动单状态(0草稿/1已提交/2已取消) =================
    public enum MovementStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        MovementStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 维修状态(0草稿/1已完成/2已取消) =================
    public enum RepairStatus {
        DRAFT(0, "草稿"),
        COMPLETED(1, "已完成"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        RepairStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
