package xyz.herz.ep.hr.enums;

/**
 * 人力资源模块通用字典枚举(参考 ERPNext HR DocType 状态机移植)。
 * <p>配合 {@link xyz.herz.ep.hr.core.HrEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "EmployeeStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 int code + String label。
 * 状态机纪律:status 字段一律 DataProxy 锁定,只允许草稿(0)状态下表单直改。
 */
public final class HrDictEnums {
    private HrDictEnums() { }

    // ================= 启停状态(主数据通用:Department/Designation/LeaveType) =================
    public enum EnableStatus {
        DISABLED(0, "停用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 性别(参考 ERPNext gender) =================
    public enum GenderType {
        MALE(0, "男"),
        FEMALE(1, "女"),
        UNKNOWN(2, "未知");
        public final int code;
        public final String label;
        GenderType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 证件类型 =================
    public enum IdType {
        ID_CARD(0, "身份证"),
        PASSPORT(1, "护照"),
        OTHER(2, "其他");
        public final int code;
        public final String label;
        IdType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 员工状态(参考 ERPNext Employee status + employment_type) =================
    // 0草稿/1在职/2试用期/3离职/4停用
    public enum EmployeeStatus {
        DRAFT(0, "草稿"),
        ACTIVE(1, "在职"),
        PROBATION(2, "试用期"),
        RESIGNED(3, "离职"),
        DISABLED(4, "停用");
        public final int code;
        public final String label;
        EmployeeStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 考勤状态(参考 ERPNext Attendance status) =================
    // 0缺勤/1出勤/2迟到/3早退/4请假
    public enum AttendanceStatus {
        ABSENT(0, "缺勤"),
        PRESENT(1, "出勤"),
        LATE(2, "迟到"),
        EARLY_LEAVE(3, "早退"),
        ON_LEAVE(4, "请假");
        public final int code;
        public final String label;
        AttendanceStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 请假状态(参考 ERPNext Leave Application status) =================
    // 0草稿/1已申请/2已批准/3已拒绝/4已取消
    public enum LeaveStatus {
        DRAFT(0, "草稿"),
        APPLIED(1, "已申请"),
        APPROVED(2, "已批准"),
        REJECTED(3, "已拒绝"),
        CANCELLED(4, "已取消");
        public final int code;
        public final String label;
        LeaveStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
