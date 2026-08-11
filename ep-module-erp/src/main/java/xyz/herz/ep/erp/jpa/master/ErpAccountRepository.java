package xyz.herz.ep.erp.jpa.master;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import xyz.herz.ep.erp.entity.master.ErpAccount;

import java.util.List;

public interface ErpAccountRepository extends JpaRepository<ErpAccount, Long> {
    @Query("SELECT a FROM ErpAccount a WHERE a.defaultFlag = true AND a.status = 1")
    List<ErpAccount> findAllDefault();
}
