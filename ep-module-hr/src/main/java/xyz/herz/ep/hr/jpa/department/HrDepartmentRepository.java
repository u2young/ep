package xyz.herz.ep.hr.jpa.department;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.department.HrDepartment;

import java.util.List;
import java.util.Optional;

public interface HrDepartmentRepository extends JpaRepository<HrDepartment, Long> {

    Optional<HrDepartment> findByCode(String code);

    List<HrDepartment> findByStatus(Integer status);

    List<HrDepartment> findByParentId(Long parentId);
}
