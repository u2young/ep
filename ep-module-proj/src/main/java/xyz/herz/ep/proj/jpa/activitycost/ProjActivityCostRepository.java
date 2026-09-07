package xyz.herz.ep.proj.jpa.activitycost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.activitycost.ProjActivityCost;

import java.util.List;

@Repository
public interface ProjActivityCostRepository extends JpaRepository<ProjActivityCost, Long> {

    /** 某项目下全部活动成本(供 totalCost 聚合)。 */
    List<ProjActivityCost> findByProjectId(Long projectId);
}
