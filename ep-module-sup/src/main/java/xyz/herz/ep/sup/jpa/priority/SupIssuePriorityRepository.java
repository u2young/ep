package xyz.herz.ep.sup.jpa.priority;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.priority.SupIssuePriority;

import java.util.List;
import java.util.Optional;

public interface SupIssuePriorityRepository extends JpaRepository<SupIssuePriority, Long> {

    Optional<SupIssuePriority> findByCode(String code);

    List<SupIssuePriority> findByStatus(Integer status);
}
