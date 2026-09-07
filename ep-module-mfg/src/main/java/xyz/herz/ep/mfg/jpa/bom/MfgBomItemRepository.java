package xyz.herz.ep.mfg.jpa.bom;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.bom.MfgBomItem;

/**
 * BOM 明细 Repository。
 * <p>注意:bom_id 列由 {@code MfgBom.items} 的
 * {@code @OneToMany @JoinColumn(name="bom_id")} 单向维护,
 * 需要按 BOM 查明细时,直接读 {@code MfgBom.getItems()} 即可。
 */
public interface MfgBomItemRepository extends JpaRepository<MfgBomItem, Long> {
}
