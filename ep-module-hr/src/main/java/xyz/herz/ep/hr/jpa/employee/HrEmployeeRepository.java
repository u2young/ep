package xyz.herz.ep.hr.jpa.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.employee.HrEmployee;

import java.util.List;
import java.util.Optional;

public interface HrEmployeeRepository extends JpaRepository<HrEmployee, Long> {

    Optional<HrEmployee> findByEmpNo(String empNo);

    List<HrEmployee> findByStatus(Integer status);

    List<HrEmployee> findByDepartmentId(Long departmentId);
}
