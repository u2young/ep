package xyz.herz.ep.crm.enums;

/**
 * CRM 通用字典枚举。
 * 通过 {@link xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler} 供 erupt {@code @ChoiceType(fetchParams)} 使用。
 * fetchParams 格式: {@code {"field":"clue_follow_status"}} 对应此处的字段名。
 */
public final class CrmDictEnums {

    private CrmDictEnums() {}

    // ========== 线索 ==========
    public enum ClueFollowStatus {
        NOT_FOLLOWED(0, "未跟进"),
        FOLLOWED(1, "已跟进");
        public final int code;
        public final String label;
        ClueFollowStatus(int code, String label) { this.code = code; this.label = label; }
    }

    public enum ClueTransformStatus {
        NOT_TRANSFORMED(0, "未转化"),
        TRANSFORMED(1, "已转化");
        public final int code;
        public final String label;
        ClueTransformStatus(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 客户 ==========
    public enum LockStatus {
        NORMAL(0, "正常"),
        LOCKED(1, "锁定(不进公海)");
        public final int code;
        public final String label;
        LockStatus(int code, String label) { this.code = code; this.label = label; }
    }

    public enum DealStatus {
        NOT_DEALT(0, "未成交"),
        DEALT(1, "已成交(不进公海)");
        public final int code;
        public final String label;
        DealStatus(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 商机 ==========
    /**
     * 商机结束状态。{@code null} 表示进行中,只能在赢单/输单/无效三选一。不可逆。
     */
    public enum BusinessEndStatus {
        WON(1, "赢单"),
        LOST(2, "输单"),
        INVALID(3, "无效");
        public final int code;
        public final String label;
        BusinessEndStatus(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 跟进记录 biz_type ==========
    /**
     * 跟进记录通用关联对象类型。配合 {@code biz_id} 字段实现一张跟进记录服务线索/客户/联系人/商机。
     */
    public enum FollowBizType {
        CLUE(10, "线索", "crm_clue"),
        CUSTOMER(20, "客户", "crm_customer"),
        CONTACT(30, "联系人", "crm_contact"),
        BUSINESS(40, "商机", "crm_business");
        public final int code;
        public final String label;
        public final String tableName;
        FollowBizType(int code, String label, String tableName) {
            this.code = code; this.label = label; this.tableName = tableName;
        }
        public static FollowBizType of(int code) {
            for (FollowBizType v : values()) if (v.code == code) return v;
            return null;
        }
    }

    // ========== 跟进方式(下次联系方式) ==========
    public enum FollowWay {
        PHONE(1, "电话"),
        VISIT(2, "上门"),
        WECHAT(3, "微信"),
        MEETING(4, "会议"),
        MAIL(5, "邮件"),
        OTHER(9, "其他");
        public final int code;
        public final String label;
        FollowWay(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 客户来源 ==========
    public enum Source {
        ONLINE_AD(1, "线上广告"),
        OFFLINE_EXPO(2, "线下展会"),
        REFERRAL(3, "转介绍"),
        COLD_CALL(4, "电话陌拜"),
        INBOUND(5, "客户主动咨询"),
        OTHER(9, "其他");
        public final int code;
        public final String label;
        Source(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 行业 ==========
    public enum Industry {
        MANUFACTURE(1, "制造业"),
        RETAIL(2, "零售批发"),
        SERVICE(3, "服务业"),
        FINANCE(4, "金融"),
        IT(5, "互联网/IT"),
        REAL_ESTATE(6, "地产/建筑"),
        MEDICAL(7, "医疗/医药"),
        EDUCATION(8, "教育"),
        GOV(9, "政府/事业单位"),
        OTHER(99, "其他");
        public final int code;
        public final String label;
        Industry(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 客户等级 ==========
    public enum CustomerLevel {
        A(10, "A 战略客户"),
        B(20, "B 重点客户"),
        C(30, "C 普通客户"),
        D(40, "D 潜在客户");
        public final int code;
        public final String label;
        CustomerLevel(int code, String label) { this.code = code; this.label = label; }
    }

    // ========== 性别 ==========
    public enum Sex {
        UNKNOWN(0, "未知"),
        MALE(1, "男"),
        FEMALE(2, "女");
        public final int code;
        public final String label;
        Sex(int code, String label) { this.code = code; this.label = label; }
    }
}
