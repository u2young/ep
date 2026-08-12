package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsAsn;

import java.util.Optional;

public interface WmsAsnRepository extends JpaRepository<WmsAsn, Long> {

    Optional<WmsAsn> findByNo(String no);
}
