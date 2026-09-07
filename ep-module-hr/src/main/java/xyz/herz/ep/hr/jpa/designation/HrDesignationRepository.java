package xyz.herz.ep.hr.jpa.designation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.designation.HrDesignation;

import java.util.List;
import java.util.Optional;

public interface HrDesignationRepository extends JpaRepository<HrDesignation, Long> {

    Optional<HrDesignation> findByCode(String code);

    List<HrDesignation> findByStatus(Integer status);
}
