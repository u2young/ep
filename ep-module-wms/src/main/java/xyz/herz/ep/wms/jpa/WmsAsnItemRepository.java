package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.herz.ep.wms.entity.WmsAsnItem;

import java.util.List;
import java.util.Optional;

public interface WmsAsnItemRepository extends JpaRepository<WmsAsnItem, Long> {

    @Query("SELECT i FROM WmsAsnItem i WHERE i.asn.id = :asnId AND i.skuCode = :skuCode")
    Optional<WmsAsnItem> findByAsnAndSku(@Param("asnId") Long asnId, @Param("skuCode") String skuCode);

    @Query("SELECT i FROM WmsAsnItem i WHERE i.asn.id = :asnId")
    List<WmsAsnItem> findByAsnId(@Param("asnId") Long asnId);
}
