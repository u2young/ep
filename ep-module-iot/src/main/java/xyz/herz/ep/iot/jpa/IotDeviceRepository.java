package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotDevice;

public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {
}
