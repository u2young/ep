package xyz.herz.ep.sup.jpa.issue;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.issue.SupIssue;

import java.util.List;

public interface SupIssueRepository extends JpaRepository<SupIssue, Long> {

    List<SupIssue> findByStatus(Integer status);

    List<SupIssue> findBySlaFulfilled(Boolean slaFulfilled);

    List<SupIssue> findByStatusIn(List<Integer> statuses);
}
