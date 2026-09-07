package xyz.herz.ep.proj.jpa.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.template.ProjProjectTemplate;

@Repository
public interface ProjProjectTemplateRepository extends JpaRepository<ProjProjectTemplate, Long> {
}
