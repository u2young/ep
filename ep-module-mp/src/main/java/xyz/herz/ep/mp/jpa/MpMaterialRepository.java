package xyz.herz.ep.mp.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mp.entity.MpMaterial;

public interface MpMaterialRepository extends JpaRepository<MpMaterial, Long> {
}
