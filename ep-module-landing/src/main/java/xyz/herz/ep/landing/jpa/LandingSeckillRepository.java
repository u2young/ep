package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.landing.entity.LandingSeckill;

import java.util.List;

public interface LandingSeckillRepository extends JpaRepository<LandingSeckill, Long> {

    List<LandingSeckill> findByStatus(Integer status);

    List<LandingSeckill> findByPageId(Long pageId);

    List<LandingSeckill> findByEnabledAndStatus(Integer enabled, Integer status);
}
