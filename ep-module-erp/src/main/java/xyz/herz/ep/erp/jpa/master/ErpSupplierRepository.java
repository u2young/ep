package xyz.herz.ep.erp.jpa.master;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.master.ErpSupplier;

import java.util.Optional;

public interface ErpSupplierRepository extends JpaRepository<ErpSupplier, Long> {
    Optional<ErpSupplier> findByCode(String code);
}
