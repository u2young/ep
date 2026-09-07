package xyz.herz.ep.hr.jpa.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.attendance.HrAttendance;

import java.util.List;
import java.util.Optional;

public interface HrAttendanceRepository extends JpaRepository<HrAttendance, Long> {

    Optional<HrAttendance> findByAttNo(String attNo);

    List<HrAttendance> findByEmployeeId(Long employeeId);

    List<HrAttendance> findByStatus(Integer status);

    Optional<HrAttendance> findByEmployeeIdAndAttDate(Long employeeId, java.time.LocalDate attDate);
}
