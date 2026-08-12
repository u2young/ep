package xyz.herz.ep.erp.jpa.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.erp.entity.finance.ErpFinanceSettlement;

import java.util.List;

public interface ErpFinanceSettlementRepository extends JpaRepository<ErpFinanceSettlement, Long> {

    /** 查询某张收/付款单的所有核销记录 */
    List<ErpFinanceSettlement> findByBizTypeAndDocId(Integer bizType, Long docId);

    /** 查询某张目标单据(采购订单/销售订单)的所有核销记录 */
    List<ErpFinanceSettlement> findByTargetBizTypeAndTargetId(Integer targetBizType, Long targetId);
}
