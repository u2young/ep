package xyz.herz.ep.erp.jpa.master;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;

import java.util.List;
import java.util.Optional;

public interface ErpWarehouseRepository extends JpaRepository<ErpWarehouse, Long> {

    @Query("SELECT w FROM ErpWarehouse w WHERE w.defaultFlag = true AND w.status = 1")
    List<ErpWarehouse> findAllDefault();

    Optional<ErpWarehouse> findByCode(String code);
}
