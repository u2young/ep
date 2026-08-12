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

    // ================= 其他入库单业务类型(整单 biz_type 字段) =================
    public enum StockInBizType {
        CHECK_GAIN(1, "盘盈"),
        TRANSFER_IN(2, "调拨入"),
        OTHER(3, "其他");
        public final int code;
        public final String label;
        StockInBizType(int c, String l) { this.code = c; this.label = l; }

        /** 映射到库存流水 StockBizType.code(入库方向,审核时使用) */
        public int toStockBizTypeCode() {
            return switch (this) {
                case CHECK_GAIN -> StockBizType.CHECK_IN.code;
                case TRANSFER_IN -> StockBizType.MOVE_IN.code;
                case OTHER -> StockBizType.OTHER_IN.code;
            };
        }

        /** 反审冲销时使用的反向(出库)StockBizType.code */
        public int toReverseStockBizTypeCode() {
            return switch (this) {
                case CHECK_GAIN -> StockBizType.CHECK_OUT.code;
                case TRANSFER_IN -> StockBizType.MOVE_OUT.code;
                case OTHER -> StockBizType.OTHER_OUT.code;
            };
        }

        public static StockInBizType of(int code) {
            for (StockInBizType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("unknown stock-in biz type code: " + code);
        }
    }

    // ================= 其他出库单业务类型(整单 biz_type 字段) =================
    public enum StockOutBizType {
        CHECK_LOSS(1, "盘亏"),
        TRANSFER_OUT(2, "调拨出"),
        OTHER(3, "其他");
        public final int code;
        public final String label;
        StockOutBizType(int c, String l) { this.code = c; this.label = l; }

        /** 映射到库存流水 StockBizType.code(出库方向,审核时使用) */
        public int toStockBizTypeCode() {
            return switch (this) {
                case CHECK_LOSS -> StockBizType.CHECK_OUT.code;
                case TRANSFER_OUT -> StockBizType.MOVE_OUT.code;
                case OTHER -> StockBizType.OTHER_OUT.code;
            };
        }

        /** 反审冲销时使用的反向(入库)StockBizType.code */
        public int toReverseStockBizTypeCode() {
            return switch (this) {
                case CHECK_LOSS -> StockBizType.CHECK_IN.code;
                case TRANSFER_OUT -> StockBizType.MOVE_IN.code;
                case OTHER -> StockBizType.OTHER_IN.code;
            };
        }

        public static StockOutBizType of(int code) {
            for (StockOutBizType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("unknown stock-out biz type code: " + code);
        }
    }

    // ================= 其他出入库单据状态(0草稿/1已审核/2已关闭) =================
    // 与通用 AuditStatus(0/20/30/99) 不同,这里只针对简化出入库单
    public enum StockDocStatus {
        DRAFT(0, "草稿"),
        APPROVED(1, "已审核"),
        CLOSED(2, "已关闭");
        public final int code;
        public final String label;
        StockDocStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 收付款单状态(0草稿/1已确认) =================
    public enum FinanceDocStatus {
        DRAFT(0, "草稿"),
        CONFIRMED(1, "已确认");
        public final int code;
        public final String label;
        FinanceDocStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
