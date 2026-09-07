package xyz.herz.ep.qal.jpa.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.qal.entity.inspection.QalInspection;

import java.util.List;
import java.util.Optional;

public interface QalInspectionRepository extends JpaRepository<QalInspection, Long> {

    Optional<QalInspection> findByInspectionNo(String inspectionNo);

    List<QalInspection> findByStatus(Integer status);

    List<QalInspection> findByItemCode(String itemCode);
}
