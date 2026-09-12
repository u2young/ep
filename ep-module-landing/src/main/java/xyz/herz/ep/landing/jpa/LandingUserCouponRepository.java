package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.landing.entity.LandingUserCoupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LandingUserCouponRepository extends JpaRepository<LandingUserCoupon, Long> {

    List<LandingUserCoupon> findByPhone(String phone);

    List<LandingUserCoupon> findByCouponIdAndStatus(Long couponId, Integer status);

    Optional<LandingUserCoupon> findByCouponIdAndPhone(Long couponId, String phone);

    /** 返回已过期且尚未标记为 EXPIRED 的优惠券(供定时任务批量更新) */
    List<LandingUserCoupon> findByStatusAndExpireTimeBefore(Integer status, LocalDateTime now);

    long countByCouponIdAndStatus(Long couponId, Integer status);

    long countByPhoneAndStatus(String phone, Integer status);
}
