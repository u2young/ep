package xyz.herz.ep.erp.jpa.product;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.product.ErpProductSku;

import java.util.Optional;

public interface ErpProductSkuRepository extends JpaRepository<ErpProductSku, Long> {
    Optional<ErpProductSku> findByCode(String code);
}
