package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpMessage;

public interface MpMessageRepository extends JpaRepository<MpMessage, Long> {
}
