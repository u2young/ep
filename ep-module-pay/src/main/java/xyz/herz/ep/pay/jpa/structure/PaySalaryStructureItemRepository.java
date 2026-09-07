package xyz.herz.ep.pay.jpa.structure;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.pay.entity.structure.PaySalaryStructureItem;

import java.util.List;

public interface PaySalaryStructureItemRepository extends JpaRepository<PaySalaryStructureItem, Long> {

    List<PaySalaryStructureItem> findByStructureId(Long structureId);
}
