package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotProduct;

public interface IotProductRepository extends JpaRepository<IotProduct, Long> {
}
