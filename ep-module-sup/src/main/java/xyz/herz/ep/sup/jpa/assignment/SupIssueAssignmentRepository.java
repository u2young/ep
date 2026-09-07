package xyz.herz.ep.sup.jpa.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.assignment.SupIssueAssignment;

import java.util.List;

public interface SupIssueAssignmentRepository extends JpaRepository<SupIssueAssignment, Long> {

    List<SupIssueAssignment> findByIssue_Id(Long issueId);
}
