package xyz.herz.ep.landing.enums;

/**
 * 落地页通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.landing.core.LandingEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "PageStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都有 int code + String label,状态值跳跃式递增以预留扩展位。
 */
public final class LandingDictEnums {
    private LandingDictEnums() { }

    // ================= 落地页状态机 =================
    public enum PageStatus {
        DRAFT(0,   "草稿"),
        PUBLISHED(10, "已发布"),
        OFFLINE(20, "已下线");
        public final int code;
        public final String label;
        PageStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 模板分类 =================
    public enum TemplateCategory {
        BLANK(1,    "空白页"),
        FORM_LEAD(2, "留资表单"),
        PRODUCT(3,  "产品介绍"),
        POSTER(4,   "海报单页"),
        ACTIVITY(5, "活动报名");
        public final int code;
        public final String label;
        TemplateCategory(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 留资来源 =================
    public enum LeadSource {
        SHORT_URL(1, "短链接"),
        SLUG(2,      "直链"),
        QRCODE(3,    "二维码");
        public final int code;
        public final String label;
        LeadSource(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 启停状态(模板通用) =================
    public enum EnableStatus {
        DISABLED(0, "禁用"),
        ENABLED(1,  "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 秒杀活动状态 =================
    public enum SeckillStatus {
        DRAFT(0,     "草稿"),
        ACTIVE(10,   "进行中"),
        ENDED(20,    "已结束");
        public final int code;
        public final String label;
        SeckillStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 秒杀订单状态 =================
    public enum SeckillOrderStatus {
        PENDING(0,    "待处理"),
        CONFIRMED(10, "已确认"),
        CANCELLED(20, "已取消");
        public final int code;
        public final String label;
        SeckillOrderStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 优惠券类型 =================
    public enum CouponType {
        FIXED_AMOUNT(1, "满减券(固定金额)"),
        DISCOUNT(2,     "折扣券(百分比折扣)");
        public final int code;
        public final String label;
        CouponType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 优惠券状态 =================
    public enum CouponStatus {
        DRAFT(0,     "草稿"),
        ENABLED(10,  "已启用"),
        EXPIRED(20,  "已过期"),
        DISABLED(30, "已禁用");
        public final int code;
        public final String label;
        CouponStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 用户优惠券状态 =================
    public enum UserCouponStatus {
        UNUSED(0,   "未使用"),
        USED(10,    "已使用"),
        EXPIRED(20, "已过期");
        public final int code;
        public final String label;
        UserCouponStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
