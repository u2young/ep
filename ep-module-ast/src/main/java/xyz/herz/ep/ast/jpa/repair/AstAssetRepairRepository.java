package xyz.herz.ep.ast.jpa.repair;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.repair.AstAssetRepair;

import java.util.List;

public interface AstAssetRepairRepository extends JpaRepository<AstAssetRepair, Long> {

    List<AstAssetRepair> findByAsset_Id(Long assetId);

    List<AstAssetRepair> findByStatus(Integer status);
}
