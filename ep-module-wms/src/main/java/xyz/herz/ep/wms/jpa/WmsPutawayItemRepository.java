package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsPutawayItem;

public interface WmsPutawayItemRepository extends JpaRepository<WmsPutawayItem, Long> {
}
