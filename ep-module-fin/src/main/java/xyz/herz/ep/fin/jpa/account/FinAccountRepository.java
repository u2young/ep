package xyz.herz.ep.fin.jpa.account;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.account.FinAccount;

import java.util.Optional;

public interface FinAccountRepository extends JpaRepository<FinAccount, Long> {
    Optional<FinAccount> findByCode(String code);
}
