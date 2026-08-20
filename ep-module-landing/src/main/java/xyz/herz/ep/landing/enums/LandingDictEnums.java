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
}
