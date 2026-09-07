package xyz.herz.ep.qal.jpa.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.qal.entity.inspection.QalInspectionItem;

import java.util.List;

public interface QalInspectionItemRepository extends JpaRepository<QalInspectionItem, Long> {

    List<QalInspectionItem> findByInspectionId(Long inspectionId);
}
