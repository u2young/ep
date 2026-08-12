package xyz.herz.ep.mall.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.mall.entity.MallProductSku;

@Repository
public interface MallProductSkuRepository extends JpaRepository<MallProductSku, Long> {
}
