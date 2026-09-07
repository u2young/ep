package xyz.herz.ep.sal.jpa.salesorder;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sal.entity.salesorder.SalSalesOrder;

/**
 * 销售订单 Repository.
 */
public interface SalSalesOrderRepository extends JpaRepository<SalSalesOrder, Long> {
    // 可按业务需求扩展自定义查询
}