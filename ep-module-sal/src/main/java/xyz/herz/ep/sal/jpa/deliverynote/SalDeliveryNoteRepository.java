package xyz.herz.ep.sal.jpa.deliverynote;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sal.entity.deliverynote.SalDeliveryNote;

/**
 * 送货单 Repository.
 */
public interface SalDeliveryNoteRepository extends JpaRepository<SalDeliveryNote, Long> {
    // 可按业务需求扩展自定义查询
}