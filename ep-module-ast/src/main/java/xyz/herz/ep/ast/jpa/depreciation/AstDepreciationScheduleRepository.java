package xyz.herz.ep.ast.jpa.depreciation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.ast.entity.depreciation.AstDepreciationSchedule;

import java.util.List;

public interface AstDepreciationScheduleRepository
        extends JpaRepository<AstDepreciationSchedule, Long> {

    List<AstDepreciationSchedule> findByAsset_Id(Long assetId);

    List<AstDepreciationSchedule> findByAsset_IdAndPosted(Long assetId, Boolean posted);
}
