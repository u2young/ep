package xyz.herz.ep.stk.jpa.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.stk.entity.batch.StkBatch;

import java.util.List;
import java.util.Optional;

/**
 * 批次 Repository(参考 ERPNext Batch 数据访问)。
 */
public interface StkBatchRepository extends JpaRepository<StkBatch, Long> {

    Optional<StkBatch> findByBatchNo(String batchNo);

    List<StkBatch> findByItemCode(String itemCode);

    List<StkBatch> findByStatus(Integer status);
}
