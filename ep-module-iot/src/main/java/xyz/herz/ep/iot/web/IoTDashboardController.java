package xyz.herz.ep.iot.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.herz.ep.iot.entity.IotDevice;
import xyz.herz.ep.iot.entity.IotAlarm;
import xyz.herz.ep.iot.enums.IotDictEnums.DeviceStatus;
import xyz.herz.ep.iot.enums.IotDictEnums.AlarmStatus;
import xyz.herz.ep.iot.jpa.IotDeviceRepository;
import xyz.herz.ep.iot.jpa.IotAlarmRepository;

import java.util.List;
import java.util.Map;

/**
 * IoT 仪表盘:返回设备在线统计和告警概况。
 * <p>
 * 前端(如 Erupt dashboard 页面或 amis 看板)通过此接口聚合展示:
 * <ul>
 *   <li>设备总数 / 在线数 / 离线数 / 禁用数</li>
 *   <li>待处理告警数</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/iot/dashboard")
public class IoTDashboardController {

    private final IotDeviceRepository deviceRepo;
    private final IotAlarmRepository alarmRepo;

    public IoTDashboardController(IotDeviceRepository deviceRepo,
                                  IotAlarmRepository alarmRepo) {
        this.deviceRepo = deviceRepo;
        this.alarmRepo = alarmRepo;
    }

    /** GET /api/iot/dashboard/stats — 返回统计 Map */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<IotDevice> devices = deviceRepo.findAll();
        long total = devices.size();
        long online = devices.stream().filter(d -> d.getStatus() != null
                && d.getStatus() == DeviceStatus.ONLINE.code).count();
        long offline = devices.stream().filter(d -> d.getStatus() != null
                && d.getStatus() == DeviceStatus.OFFLINE.code).count();
        long disabled = devices.stream().filter(d -> d.getStatus() != null
                && d.getStatus() == DeviceStatus.DISABLED.code).count();

        List<IotAlarm> alarms = alarmRepo.findAll();
        long pendingAlarms = alarms.stream().filter(a -> a.getStatus() != null
                && a.getStatus() == AlarmStatus.PENDING.code).count();
        long processingAlarms = alarms.stream().filter(a -> a.getStatus() != null
                && a.getStatus() == AlarmStatus.PROCESSING.code).count();

        return Map.of(
                "deviceTotal", total,
                "deviceOnline", online,
                "deviceOffline", offline,
                "deviceDisabled", disabled,
                "alarmPending", pendingAlarms,
                "alarmProcessing", processingAlarms
        );
    }
}
