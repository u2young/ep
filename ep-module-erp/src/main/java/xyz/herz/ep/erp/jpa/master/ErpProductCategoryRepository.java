package xyz.herz.ep.erp.jpa.master;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;

public interface ErpProductCategoryRepository extends JpaRepository<ErpProductCategory, Long> {
}
