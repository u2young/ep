package xyz.herz.ep.mfg.enums;

/**
 * 生产制造模块通用字典枚举(参考 ERPNext Manufacturing DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "WorkOrderStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class MfgDictEnums {
    private MfgDictEnums() { }

    // ================= 启停状态(主数据通用:BOM/工序/工作中心) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 工单状态(0草稿/1未开工/2在制/3已完工/4已停工/5取消) =================
    public enum WorkOrderStatus {
        DRAFT(0, "草稿"),
        NOT_STARTED(1, "未开工"),
        IN_PRODUCTION(2, "在制"),
        COMPLETED(3, "已完工"),
        STOPPED(4, "已停工"),
        CANCELLED(5, "已取消");
        public final int code;
        public final String label;
        WorkOrderStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 派工单状态(0待开工/1在制/2已完工/3已取消) =================
    public enum JobCardStatus {
        PENDING(0, "待开工"),
        IN_PRODUCTION(1, "在制"),
        COMPLETED(2, "已完工"),
        CANCELLED(3, "已取消");
        public final int code;
        public final String label;
        JobCardStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 库存出入单类型(ttype:1领料/2退料/3完工入/4委外发/5委外收) =================
    public enum StockEntryType {
        MATERIAL_ISSUE(1, "生产领料"),
        MATERIAL_RETURN(2, "退料入库"),
        COMPLETION_IN(3, "完工入库"),
        SUBCONTRACT_ISSUE(4, "委外发料"),
        SUBCONTRACT_RECEIPT(5, "委外收货");
        public final int code;
        public final String label;
        StockEntryType(int c, String l) { this.code = c; this.label = l; }

        /**
         * 映射到 ERP {@code ErpDictEnums.StockBizType} 的 code,
         * 用于 InventoryChangeFacade.change 调用时填充 bizType。
         * 退料入库走 MATERIAL_RETURN,在 InventoryChangeFacade 层按方向(正负)处理,
         * 这里映射到对应的制造入库/出库业务类型。
         */
        public int stockBizTypeCode() {
            return switch (this) {
                case MATERIAL_ISSUE -> 40;       // MANUFACTURE_OUT 生产领料
                case MATERIAL_RETURN -> 41;     // MANUFACTURE_IN  退料视同入库方向
                case COMPLETION_IN -> 41;       // MANUFACTURE_IN  完工入库
                case SUBCONTRACT_ISSUE -> 42;   // SUBCONTRACT_OUT 委外发料
                case SUBCONTRACT_RECEIPT -> 43; // SUBCONTRACT_IN  委外收货
            };
        }

        public static StockEntryType of(int code) {
            for (StockEntryType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("unknown stock entry type code: " + code);
        }
    }

    // ================= 库存出入单状态(0草稿/1已审核/2已取消) =================
    public enum StockEntryStatus {
        DRAFT(0, "草稿"),
        AUDITED(1, "已审核"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        StockEntryStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 委外单状态(0草稿/1已下单/2部分到货/3全部到货/4已结算/5取消) =================
    public enum SubcontractingStatus {
        DRAFT(0, "草稿"),
        ORDERED(1, "已下单"),
        PARTIAL_RECEIVED(2, "部分到货"),
        FULL_RECEIVED(3, "全部到货"),
        SETTLED(4, "已结算"),
        CANCELLED(5, "已取消");
        public final int code;
        public final String label;
        SubcontractingStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
