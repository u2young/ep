package xyz.herz.ep.crm.core;

import xyz.herz.ep.crm.enums.CrmDictEnums.FollowBizType;
import xyz.herz.ep.crm.jpa.CrmFollowUpRecordRepository;
import xyz.herz.ep.crm.jpa.CrmClueRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import xyz.herz.ep.crm.jpa.CrmContactRepository;
import xyz.herz.ep.crm.jpa.CrmBusinessRepository;
import xyz.herz.ep.crm.entity.CrmFollowUpRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * CRM 通用服务:
 * <ul>
 *   <li>写跟进记录(通用 biz_type + biz_id),并自动刷新对应主体的「最后跟进时间/内容」汇总字段。</li>
 *   <li>公海回收扫描(由定时任务调用)</li>
 * </ul>
 *
 * todo:后续合同回款模块只需要在 FollowBizType 枚举里增加枚举值 + 写主体刷新逻辑,不需要改调用方。
 */
@Service
public class CrmFollowService {

    @Resource private CrmFollowUpRecordRepository followRepo;
    @Resource private CrmClueRepository clueRepo;
    @Resource private CrmCustomerRepository customerRepo;
    @Resource private CrmContactRepository contactRepo;
    @Resource private CrmBusinessRepository businessRepo;

    @Transactional
    public CrmFollowUpRecord createFollow(FollowBizType bizType, Long bizId,
                                          Integer followWay, String content, Long creator,
                                          LocalDateTime contactNextTime) {
        CrmFollowUpRecord rec = new CrmFollowUpRecord();
        rec.setBizType(bizType.code);
        rec.setBizId(bizId);
        rec.setFollowWay(followWay);
        rec.setContent(content);
        rec.setFollowTime(LocalDateTime.now());
        followRepo.save(rec);
        refreshFollowSummary(bizType, bizId, followWay, content, contactNextTime);
        return rec;
    }

    /**
     * 把本次跟进汇总回写到主体,方便列表/待办直接查询,避免频繁 JOIN。
     * 这就是 CRM 跟进记录"双写"的设计:流水 append-only + 主体冗余汇总。
     */
    private void refreshFollowSummary(FollowBizType bizType, Long bizId,
                                      Integer followWay, String content, LocalDateTime next) {
        LocalDateTime now = LocalDateTime.now();
        switch (bizType) {
            case CLUE -> clueRepo.findById(bizId).ifPresent(c -> {
                c.setFollowUpStatus(1);
                c.setContactLastTime(now);
                c.setContactLastContent(truncate(content, 255));
                if (next != null) c.setContactNextTime(next);
                clueRepo.save(c);
            });
            case CUSTOMER -> customerRepo.findById(bizId).ifPresent(c -> {
                c.setFollowUpStatus(1);
                c.setContactLastTime(now);
                c.setContactLastContent(truncate(content, 255));
                if (next != null) c.setContactNextTime(next);
                customerRepo.save(c);
            });
            case CONTACT -> contactRepo.findById(bizId).ifPresent(c -> {
                c.setFollowUpStatus(1);
                c.setContactLastTime(now);
                c.setContactLastContent(truncate(content, 255));
                if (next != null) c.setContactNextTime(next);
                contactRepo.save(c);
            });
            case BUSINESS -> businessRepo.findById(bizId).ifPresent(b -> {
                b.setFollowUpStatus(1);
                b.setContactLastTime(now);
                b.setContactLastContent(truncate(content, 255));
                if (next != null) b.setContactNextTime(next);
                businessRepo.save(b);
            });
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
