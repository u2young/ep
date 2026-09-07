package xyz.herz.ep.hr.jpa.leavetype;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.hr.entity.leavetype.HrLeaveType;

import java.util.List;
import java.util.Optional;

public interface HrLeaveTypeRepository extends JpaRepository<HrLeaveType, Long> {

    Optional<HrLeaveType> findByCode(String code);

    List<HrLeaveType> findByStatus(Integer status);
}
