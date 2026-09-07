package xyz.herz.ep.hr.jpa.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.leave.HrLeaveApplication;

import java.util.List;
import java.util.Optional;

public interface HrLeaveApplicationRepository extends JpaRepository<HrLeaveApplication, Long> {

    Optional<HrLeaveApplication> findByLeaveNo(String leaveNo);

    List<HrLeaveApplication> findByEmployeeId(Long employeeId);

    List<HrLeaveApplication> findByStatus(Integer status);
}
