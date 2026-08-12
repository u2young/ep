package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpUser;

import java.util.Optional;

public interface MpUserRepository extends JpaRepository<MpUser, Long> {
    Optional<MpUser> findByOpenId(String openId);
}
