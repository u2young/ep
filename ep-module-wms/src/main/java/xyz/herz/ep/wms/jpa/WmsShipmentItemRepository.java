package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.herz.ep.wms.entity.WmsShipmentItem;

import java.util.List;
import java.util.Optional;

public interface WmsShipmentItemRepository extends JpaRepository<WmsShipmentItem, Long> {

    @Query("SELECT i FROM WmsShipmentItem i WHERE i.notice.id = :noticeId AND i.skuCode = :skuCode")
    Optional<WmsShipmentItem> findByNoticeAndSku(@Param("noticeId") Long noticeId, @Param("skuCode") String skuCode);

    @Query("SELECT i FROM WmsShipmentItem i WHERE i.notice.id = :noticeId")
    List<WmsShipmentItem> findByNoticeId(@Param("noticeId") Long noticeId);
}
