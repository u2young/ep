package xyz.herz.ep.iot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.iot.entity.IotProductCategory;

public interface IotProductCategoryRepository extends JpaRepository<IotProductCategory, Long> {
}
