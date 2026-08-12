package xyz.herz.ep.mall.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.mall.entity.MallTradeOrder;

@Repository
public interface MallTradeOrderRepository extends JpaRepository<MallTradeOrder, Long> {
}
