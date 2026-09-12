package xyz.herz.ep.landing.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.landing.entity.LandingCoupon;

import java.util.Optional;

public interface LandingCouponRepository extends JpaRepository<LandingCoupon, Long> {

    Optional<LandingCoupon> findByCouponCode(String couponCode);
}
