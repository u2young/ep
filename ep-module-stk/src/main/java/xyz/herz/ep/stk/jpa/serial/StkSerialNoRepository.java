package xyz.herz.ep.stk.jpa.serial;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.stk.entity.serial.StkSerialNo;

import java.util.List;
import java.util.Optional;

/**
 * 序列号 Repository(参考 ERPNext Serial No 数据访问)。
 */
public interface StkSerialNoRepository extends JpaRepository<StkSerialNo, Long> {

    Optional<StkSerialNo> findBySerialNo(String serialNo);

    List<StkSerialNo> findByItemCode(String itemCode);

    List<StkSerialNo> findByBatchNo(String batchNo);

    List<StkSerialNo> findByStatus(Integer status);
}
