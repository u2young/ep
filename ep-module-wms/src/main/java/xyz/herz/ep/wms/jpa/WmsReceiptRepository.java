package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsReceipt;

import java.util.Optional;

public interface WmsReceiptRepository extends JpaRepository<WmsReceipt, Long> {

    Optional<WmsReceipt> findByNo(String no);
}
