package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsReceiptItem;

public interface WmsReceiptItemRepository extends JpaRepository<WmsReceiptItem, Long> {
}
