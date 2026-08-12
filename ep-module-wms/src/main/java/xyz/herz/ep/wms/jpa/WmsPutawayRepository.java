package xyz.herz.ep.wms.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.wms.entity.WmsPutaway;

import java.util.Optional;

public interface WmsPutawayRepository extends JpaRepository<WmsPutaway, Long> {

    Optional<WmsPutaway> findByNo(String no);
}
