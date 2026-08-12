package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpMenu;

public interface MpMenuRepository extends JpaRepository<MpMenu, Long> {
}
