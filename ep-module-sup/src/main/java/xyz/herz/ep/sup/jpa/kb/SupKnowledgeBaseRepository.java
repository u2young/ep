package xyz.herz.ep.sup.jpa.kb;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.sup.entity.kb.SupKnowledgeBase;

import java.util.List;

public interface SupKnowledgeBaseRepository extends JpaRepository<SupKnowledgeBase, Long> {

    List<SupKnowledgeBase> findByStatus(Integer status);
}
