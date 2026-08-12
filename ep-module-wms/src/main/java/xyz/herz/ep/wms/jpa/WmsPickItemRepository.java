package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsPickItem;

public interface WmsPickItemRepository extends JpaRepository<WmsPickItem, Long> {
}
