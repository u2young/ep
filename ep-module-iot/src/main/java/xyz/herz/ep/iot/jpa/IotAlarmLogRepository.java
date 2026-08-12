package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotAlarmLog;

public interface IotAlarmLogRepository extends JpaRepository<IotAlarmLog, Long> {
}
