package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.landing.entity.LandingSeckillOrder;
import xyz.herz.ep.landing.entity.LandingUserCoupon;

import java.util.List;
import java.util.Optional;

public interface LandingSeckillOrderRepository extends JpaRepository<LandingSeckillOrder, Long> {

    List<LandingSeckillOrder> findBySeckillId(Long seckillId);

    List<LandingSeckillOrder> findByPhone(String phone);

    Optional<LandingSeckillOrder> findBySeckillIdAndPhone(Long seckillId, String phone);

    long countBySeckillId(Long seckillId);

    long countBySeckillIdAndPhone(Long seckillId, String phone);

    long countBySeckillIdAndStatus(Long seckillId, Integer status);
}
