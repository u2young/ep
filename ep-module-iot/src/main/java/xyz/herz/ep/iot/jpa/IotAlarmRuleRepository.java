package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotAlarmRule;

public interface IotAlarmRuleRepository extends JpaRepository<IotAlarmRule, Long> {
}
