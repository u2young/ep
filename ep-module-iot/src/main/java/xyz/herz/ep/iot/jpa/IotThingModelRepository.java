package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotThingModel;

public interface IotThingModelRepository extends JpaRepository<IotThingModel, Long> {
}
