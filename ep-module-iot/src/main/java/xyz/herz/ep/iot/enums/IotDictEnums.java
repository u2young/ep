package xyz.herz.ep.iot.enums;

/**
 * IoT 通用字典枚举。
 * <p>配合 {@link xyz.herz.ep.iot.core.IotEnumChoiceFetchHandler} 使用,
 * 下拉通过 {@code @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
 *   fetchHandlerParams = "DeviceStatus")} 取枚举列表。
 *
 * <p>编码规则:每个枚举都必须有 code(int 或 String) + String label。
 * DeviceStatus / ThingModelType / AccessMode / NodeType / AlarmLevel / AlarmStatus / EnableStatus 为 int code;
 * DataType / NetType 为 String code。
 */
public final class IotDictEnums {
    private IotDictEnums() { }

    // ================= 设备状态(生命周期状态机) =================
    public enum DeviceStatus {
        INACTIVE(0, "未激活"),
        ONLINE(1, "在线"),
        OFFLINE(2, "离线"),
        DISABLED(3, "禁用");
        public final int code;
        public final String label;
        DeviceStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 物模型类型 =================
    public enum ThingModelType {
        PROPERTY(1, "属性"),
        SERVICE(2, "服务"),
        EVENT(3, "事件");
        public final int code;
        public final String label;
        ThingModelType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 物模型数据类型(String code) =================
    public enum DataType {
        INT("INT", "整数"),
        FLOAT("FLOAT", "浮点"),
        STRING("STRING", "字符串"),
        BOOL("BOOL", "布尔"),
        ENUM("ENUM", "枚举");
        public final String code;
        public final String label;
        DataType(String c, String l) { this.code = c; this.label = l; }
    }

    // ================= 物模型访问模式 =================
    public enum AccessMode {
        READ_WRITE(1, "读写"),
        READ_ONLY(2, "只读");
        public final int code;
        public final String label;
        AccessMode(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 产品节点类型 =================
    public enum NodeType {
        DIRECT(1, "直连设备"),
        GATEWAY(2, "网关"),
        SUB(3, "子设备");
        public final int code;
        public final String label;
        NodeType(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 联网方式(String code) =================
    public enum NetType {
        WIFI("WIFI", "Wi-Fi"),
        CELLULAR("CELLULAR", "蜂窝"),
        ETHERNET("ETHERNET", "以太网"),
        OTHER("OTHER", "其他");
        public final String code;
        public final String label;
        NetType(String c, String l) { this.code = c; this.label = l; }
    }

    // ================= 告警级别 =================
    public enum AlarmLevel {
        INFO(1, "提示"),
        WARNING(2, "警告"),
        CRITICAL(3, "严重");
        public final int code;
        public final String label;
        AlarmLevel(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 告警状态(状态机) =================
    public enum AlarmStatus {
        PENDING(0, "待处理"),
        PROCESSING(10, "处理中"),
        RESOLVED(20, "已解决"),
        IGNORED(30, "已忽略");
        public final int code;
        public final String label;
        AlarmStatus(int c, String l) { this.code = c; this.label = l; }
    }

    // ================= 启停状态(通用) =================
    public enum EnableStatus {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");
        public final int code;
        public final String label;
        EnableStatus(int c, String l) { this.code = c; this.label = l; }
    }
}
