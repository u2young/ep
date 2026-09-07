package xyz.herz.ep.mfg.jpa.operation;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.mfg.entity.operation.MfgOperation;

import java.util.List;
import java.util.Optional;

public interface MfgOperationRepository extends JpaRepository<MfgOperation, Long> {

    Optional<MfgOperation> findByNo(String no);

    /** 按工作中心查工序。 */
    List<MfgOperation> findByWorkstationId(Long workstationId);

    /** 按状态查启用的工序。 */
    List<MfgOperation> findByStatus(Integer status);
}
