package xyz.herz.ep.erp.jpa.product;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.product.ErpProductBarCode;

public interface ErpProductBarCodeRepository extends JpaRepository<ErpProductBarCode, Long> {
}
