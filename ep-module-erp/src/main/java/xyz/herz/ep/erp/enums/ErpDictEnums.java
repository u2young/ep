package xyz.herz.ep.erp.enums;

import java.math.BigDecimal;

/**
 * ERP 通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "AuditStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。参考 yudao + 完整状态机两套级别合并。
 */
public final class ErpDictEnums {
    private ErpDictEnums() { }

    // ================= 启停状态(主数据通用) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 产品上架状态 =================
    public enum ProductListingStatus {
        LISTED(1, "上架"),
        DELISTED(0, "下架");
        public final int code;
        public final String label;
        ProductListingStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 审批状态机(yudao 极简 + 扩展关闭/作废) =================
    public enum AuditStatus {
        DRAFT(0, "草稿"),
        APPROVED(20, "已审批"),
        CLOSED(30, "关闭"),
        VOIDED(99, "作废");
        public final int code;
        public final String label;
        AuditStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 执行状态(派生显示,不是落库字段) =================
    public enum ExecStatus {
        NONE(0, "未执行"),
        PARTIAL(1, "部分执行"),
        FULL(2, "全部执行");
        public final int code;
        public final String label;
        ExecStatus(int c, String l) { this.code = c; this.label = l; }
    }

    /**
     * 通用派生:已执行数/总数 → ExecStatus。
     * 总数 <= 0 返回 NONE;已执行 == 0 返回 NONE;已执行 < total PARTIAL;否则 FULL。
     */
    public static int deriveExec(BigDecimal executed, BigDecimal total) {
        if (total == null || total.signum() <= 0) return ExecStatus.NONE.code;
        if (executed == null) executed = BigDecimal.ZERO;
        int cmp = executed.compareTo(total);
        if (cmp >= 0) return ExecStatus.FULL.code;
        return executed.signum() == 0 ? ExecStatus.NONE.code : ExecStatus.PARTIAL.code;
    }

    // ================= 库存业务类型(流水 biz_type)与 ERP Facade 共用 1:1 映射 =================
    public enum StockBizType {
        // 入库 (>0)
        OTHER_IN(10, "其它入库"),
        PURCHASE_IN(11, "采购入库"),
        SALE_RETURN_IN(12, "销售退货入库"),
        CHECK_IN(13, "盘盈入库"),
        MOVE_IN(31, "调拨入库"),
        // 出库 (<0)
        OTHER_OUT(20, "其它出库"),
        PURCHASE_RETURN_OUT(21, "采购退货出库"),
        SALE_OUT(22, "销售出库"),
        CHECK_OUT(23, "盘亏出库"),
        MOVE_OUT(30, "调拨出库");
        public final int code;
        public final String label;
        StockBizType(int c, String l) { this.code = c; this.label = l; }

        /** 正数入库=true,负数出库=false,用于库存变更方向校验 */
        public boolean inbound() {
            return this == OTHER_IN || this == PURCHASE_IN || this == SALE_RETURN_IN
                || this == CHECK_IN || this == MOVE_IN;
        }

        public static StockBizType of(int code) {
            for (StockBizType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("unknown stock biz type code: " + code);
        }
    }

    // ================= 产品成本核算方式(预留,当前默认加权平均) =================
    public enum CostMethod {
        WEIGHTED_AVG(1, "移动加权平均"),
        FIFO(2, "先进先出"),
        BATCH(3, "分批认定");
        public final int code;
        public final String label;
        CostMethod(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 结算方式(P0 默认应收/应付记账,留扩展) =================
    public enum SettleMethod {
        CREDIT(1, "赊账/月结"),
        SPOT(2, "现款现货"),
        PREPAID(3, "预付/预收");
        public final int code;
        public final String label;
        SettleMethod(int c, String l) { this.code = c; this.label = l; }
    }
}
