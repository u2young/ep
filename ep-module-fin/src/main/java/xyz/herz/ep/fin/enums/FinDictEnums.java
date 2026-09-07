package xyz.herz.ep.fin.enums;

/**
 * 财务模块通用字典枚举(参考 ERPNext Accounting DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.fin.core.FinEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "JournalStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 */
public final class FinDictEnums {
    private FinDictEnums() { }

    // ================= 启停状态(主数据通用:科目/成本中心) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 会计科目类型(参考 ERPNext Account Type) =================
    public enum AccountType {
        ASSET(1, "资产"),
        LIABILITY(2, "负债"),
        EQUITY(3, "权益"),
        INCOME(4, "收入"),
        EXPENSE(5, "支出"),
        COST(6, "成本");
        public final int code;
        public final String label;
        AccountType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 凭证状态(0草稿/1已提交/2已取消) =================
    public enum JournalStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        JournalStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 销售/采购发票状态(0草稿/1已提交/2已付款/3部分付款/4取消) =================
    public enum InvoiceStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        PAID(2, "已付款"),
        PARTIAL_PAID(3, "部分付款"),
        CANCELLED(4, "已取消");
        public final int code;
        public final String label;
        InvoiceStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 收付款单状态(0草稿/1已提交/2已取消) =================
    public enum PaymentStatus {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "已提交"),
        CANCELLED(2, "已取消");
        public final int code;
        public final String label;
        PaymentStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 收付款类型(1收/2付/3内部转) =================
    public enum PaymentType {
        RECEIVE(1, "收款"),
        PAY(2, "付款"),
        INTERNAL_TRANSFER(3, "内部转账");
        public final int code;
        public final String label;
        PaymentType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 预算状态(0草稿/1已批准/2已关闭) =================
    public enum BudgetStatus {
        DRAFT(0, "草稿"),
        APPROVED(1, "已批准"),
        CLOSED(2, "已关闭");
        public final int code;
        public final String label;
        BudgetStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 凭证来源类型(用于 source_type 快照反查) =================
    public enum JournalSourceType {
        MANUAL_JOURNAL("ManualJournal", "手工凭证"),
        SALES_INVOICE("SalesInvoice", "销售发票"),
        PURCHASE_INVOICE("PurchaseInvoice", "采购发票"),
        PAYMENT_ENTRY("PaymentEntry", "收付款单"),
        ASSET_DEPRECIATION("AssetDepreciation", "资产折旧"),
        PROJECT_REVENUE("ProjectRevenue", "项目收入确认"),
        PROJECT_EXPENSE("ProjectExpense", "项目费用入账"),
        ASSET_DISPOSAL("AssetDisposal", "资产处置"),
        SALARY("Salary", "工资过账");
        public final String code;
        public final String label;
        JournalSourceType(String c, String l) { this.code = c; this.label = l; }
    }
}
