package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CrmCustomerRepository extends JpaRepository<CrmCustomer, Long> {
    long countByOwnerUserId(Long ownerUserId);
    long countByDealStatus(Integer dealStatus);
    long countByOwnerUserIdNull();

    /** 公海回收候选:ownerUserId 非空 + 未锁定 + 未成交 + (最后跟进早于阈值 OR 成为负责人早于阈值) */
    @Query("select c from CrmCustomer c where c.ownerUserId is not null and c.lockStatus = 0 and c.dealStatus = 0 " +
        "and (c.contactLastTime is null or c.contactLastTime < :contactExpire) " +
        "and (c.ownerTime is null or c.ownerTime < :dealExpire)")
    List<CrmCustomer> findExpireRecycle(LocalDateTime contactExpire, LocalDateTime dealExpire);
}
