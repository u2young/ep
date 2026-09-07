package xyz.herz.ep.ast.jpa.movement;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.movement.AstAssetMovement;

import java.util.List;

public interface AstAssetMovementRepository extends JpaRepository<AstAssetMovement, Long> {

    List<AstAssetMovement> findByAsset_Id(Long assetId);

    List<AstAssetMovement> findByStatus(Integer status);
}
