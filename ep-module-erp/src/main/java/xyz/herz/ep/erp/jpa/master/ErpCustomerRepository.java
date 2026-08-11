package xyz.herz.ep.erp.jpa.master;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.master.ErpCustomer;

import java.util.Optional;

public interface ErpCustomerRepository extends JpaRepository<ErpCustomer, Long> {
    Optional<ErpCustomer> findByCode(String code);
}
