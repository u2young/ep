package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpAccount;

import java.util.Optional;

public interface MpAccountRepository extends JpaRepository<MpAccount, Long> {
    Optional<MpAccount> findByAppId(String appId);
}
