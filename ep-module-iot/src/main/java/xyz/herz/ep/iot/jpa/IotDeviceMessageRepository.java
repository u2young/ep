package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotDeviceMessage;

public interface IotDeviceMessageRepository extends JpaRepository<IotDeviceMessage, Long> {
}
