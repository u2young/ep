package xyz.herz.ep.ast.jpa.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.asset.AstAsset;

import java.util.List;
import java.util.Optional;

public interface AstAssetRepository extends JpaRepository<AstAsset, Long> {

    Optional<AstAsset> findByAssetNo(String assetNo);

    List<AstAsset> findByStatus(Integer status);

    List<AstAsset> findByStatusIn(List<Integer> statuses);

    List<AstAsset> findByCategoryId(Long categoryId);
}
