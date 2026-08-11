package xyz.herz.ep.erp.jpa.product;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.product.ErpProduct;

import java.util.Optional;

public interface ErpProductRepository extends JpaRepository<ErpProduct, Long> {
    Optional<ErpProduct> findByCode(String code);
}
