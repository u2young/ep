package xyz.herz.ep.qal.jpa.criteria;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.qal.entity.criteria.QalCriteria;

import java.util.List;
import java.util.Optional;

public interface QalCriteriaRepository extends JpaRepository<QalCriteria, Long> {

    Optional<QalCriteria> findByCode(String code);

    List<QalCriteria> findByStatus(Integer status);
}
