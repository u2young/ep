package xyz.herz.ep.proj.jpa.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.timesheet.ProjTimesheet;

@Repository
public interface ProjTimesheetRepository extends JpaRepository<ProjTimesheet, Long> {
}
