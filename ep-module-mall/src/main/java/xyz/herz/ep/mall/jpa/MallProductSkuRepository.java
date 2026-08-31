package xyz.herz.ep.mall.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.mall.entity.MallProductSku;
import xyz.herz.ep.mall.entity.MallProductSpu;

import java.util.List;

@Repository
public interface MallProductSkuRepository extends JpaRepository<MallProductSku, Long> {
    /** 按 SPU 查询其所有 SKU(按生成顺序 id 升序)。 */
    List<MallProductSku> findByProductOrderByIdAsc(MallProductSpu product);

    long countByProduct(MallProductSpu product);
}
