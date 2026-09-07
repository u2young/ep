package xyz.herz.ep.sal.jpa.salesperson;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sal.entity.salesperson.SalSalesPerson;

/**
 * 销售员 Repository.
 */
public interface SalSalesPersonRepository extends JpaRepository<SalSalesPerson, Long> {
    // 可按业务需求扩展自定义查询
}