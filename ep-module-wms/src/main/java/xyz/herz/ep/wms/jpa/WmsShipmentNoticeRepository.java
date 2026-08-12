package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;

import java.util.Optional;

public interface WmsShipmentNoticeRepository extends JpaRepository<WmsShipmentNotice, Long> {

    Optional<WmsShipmentNotice> findByNo(String no);
}
