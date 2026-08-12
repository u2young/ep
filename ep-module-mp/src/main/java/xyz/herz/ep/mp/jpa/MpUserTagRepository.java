package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpUserTag;

public interface MpUserTagRepository extends JpaRepository<MpUserTag, Long> {
}
