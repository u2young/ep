package xyz.herz.ep.mp.enums;

/**
 * 公众号(MP)通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "EnableStatus")} 取枚举列表。
 *
 * <p>编码规则:
 * <ul>
 *   <li>数值型状态枚举: {@code public final int code} + {@code String label}</li>
 *   <li>字符串型类型枚举(MaterialType/ReplyContentType/MessageType): {@code public final String code} + {@code String label}</li>
 * </ul>
 */
public final class MpDictEnums {
    private MpDictEnums() { }

    // ================= 粉丝关注状态 =================
    public enum SubscribeStatus {
        UNSUBSCRIBED(0, "未关注"),
        SUBSCRIBED(1, "已关注");
        public final int code;
        public final String label;
        SubscribeStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 菜单状态 =================
    public enum MenuStatus {
        DRAFT(0, "草稿"),
        PUBLISHED(1, "已发布");
        public final int code;
        public final String label;
        MenuStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 素材类型(String code) =================
    public enum MaterialType {
        IMAGE("IMAGE", "图片"),
        VOICE("VOICE", "语音"),
        VIDEO("VIDEO", "视频"),
        THUMB("THUMB", "缩略图");
        public final String code;
        public final String label;
        MaterialType(String c, String l) { this.code = c; this.label = l; }
    }

    // ================= 自动回复类型 =================
    public enum ReplyType {
        SUBSCRIBE(1, "关注回复"),
        KEYWORD(2, "关键字回复"),
        DEFAULT(3, "默认回复");
        public final int code;
        public final String label;
        ReplyType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 回复内容类型(String code) =================
    public enum ReplyContentType {
        TEXT("TEXT", "文本"),
        IMAGE("IMAGE", "图片"),
        NEWS("NEWS", "图文");
        public final String code;
        public final String label;
        ReplyContentType(String c, String l) { this.code = c; this.label = l; }
    }

    // ================= 启停状态(通用) =================
    public enum EnableStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 消息类型(String code) =================
    public enum MessageType {
        TEXT("TEXT", "文本"),
        IMAGE("IMAGE", "图片"),
        VOICE("VOICE", "语音"),
        VIDEO("VIDEO", "视频"),
        NEWS("NEWS", "图文"),
        EVENT("EVENT", "事件");
        public final String code;
        public final String label;
        MessageType(String c, String l) { this.code = c; this.label = l; }
    }

    // ================= 消息方向 =================
    public enum MessageDirection {
        RECEIVE(1, "接收"),
        SEND(2, "发送");
        public final int code;
        public final String label;
        MessageDirection(int c, String l) { this.code = c; this.label = l; }
    }
}
