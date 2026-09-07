package xyz.herz.ep.proj.jpa.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.project.ProjProject;

import java.math.BigDecimal;

@Repository
public interface ProjProjectRepository extends JpaRepository<ProjProject, Long> {

    /** 项目累计成本回写后,便于校验 totalCost = sum(activity_cost.amount)。 */
    long countByCustomerId(Long customerId);
}
