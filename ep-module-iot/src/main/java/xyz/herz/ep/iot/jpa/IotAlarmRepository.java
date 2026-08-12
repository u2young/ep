package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotAlarm;

public interface IotAlarmRepository extends JpaRepository<IotAlarm, Long> {
}
