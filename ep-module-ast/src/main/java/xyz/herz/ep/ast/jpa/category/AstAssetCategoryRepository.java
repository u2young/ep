package xyz.herz.ep.ast.jpa.category;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;

import java.util.List;
import java.util.Optional;

public interface AstAssetCategoryRepository extends JpaRepository<AstAssetCategory, Long> {

    Optional<AstAssetCategory> findByCode(String code);

    List<AstAssetCategory> findByStatus(Integer status);
}
