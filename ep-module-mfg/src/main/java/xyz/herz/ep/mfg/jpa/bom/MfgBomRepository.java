package xyz.herz.ep.mfg.jpa.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.bom.MfgBom;

import java.util.List;
import java.util.Optional;

public interface MfgBomRepository extends JpaRepository<MfgBom, Long> {

    /** 按产品查生效的 BOM(status=1 启用)。 */
    List<MfgBom> findByProductIdAndIsActiveTrue(Long productId);

    /** 产品的默认 BOM。 */
    Optional<MfgBom> findByProductIdAndIsDefaultTrue(Long productId);

    Optional<MfgBom> findByNo(String no);
}
