package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmClue;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmClueRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.erupt.jpa.dao.EruptDao;

import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索「转化为客户」按钮处理器。
 *
 * 副作用(原子性,一个事务内):
 * 1. 新建客户 crm_customer(拷线索基本信息、联系方式、负责人、字典字段);
 * 2. 线索 transformStatus=1、写 customerId;
 * 3. todo:若有联系人线索信息,可以再建一条 CrmContact(当前 MVP 略,后续扩展不用改本 Handler,追加即可)。
 */
@Component
public class CrmClueTransformHandler implements OperationHandler<CrmClue, String> {

    @Resource private CrmClueRepository clueRepo;
    @Resource private CrmCustomerRepository customerRepo;
    @Resource private EruptDao eruptDao;

    @Override
    @Transactional
    public String exec(List<CrmClue> data, String param, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (CrmClue clue : data) {
            try {
                if (clue.getTransformStatus() != null && clue.getTransformStatus() == 1) {
                    fail++;
                    sb.append("线索[").append(clue.getName()).append("] 已转化过,跳过。\n");
                    continue;
                }
                // 1. 创建客户
                CrmCustomer c = new CrmCustomer();
                c.setName(clue.getName());
                c.setOwnerUserId(clue.getOwnerUserId());
                c.setOwnerTime(LocalDateTime.now());
                c.setLockStatus(CrmDictEnums.LockStatus.NORMAL.code);
                c.setDealStatus(CrmDictEnums.DealStatus.NOT_DEALT.code);
                c.setFollowUpStatus(CrmDictEnums.ClueFollowStatus.NOT_FOLLOWED.code);
                c.setMobile(clue.getMobile());
                c.setTelephone(clue.getTelephone());
                c.setQq(clue.getQq());
                c.setWechat(clue.getWechat());
                c.setEmail(clue.getEmail());
                c.setAreaId(clue.getAreaId());
                c.setDetailAddress(clue.getDetailAddress());
                c.setIndustryId(clue.getIndustryId());
                c.setLevel(clue.getLevel());
                c.setSource(clue.getSource());
                c.setRemark("由线索转化生成。线索ID=" + clue.getId() + " / " + (clue.getRemark() == null ? "" : clue.getRemark()));
                customerRepo.save(c);

                // 2. 回写线索
                clue.setTransformStatus(CrmDictEnums.ClueTransformStatus.TRANSFORMED.code);
                clue.setCustomerId(c.getId());
                clueRepo.save(clue);
                ok++;
            } catch (Exception e) {
                fail++;
                sb.append("线索[").append(clue.getName()).append("] 转化失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "转化完成:成功 " + ok + " 条,失败 " + fail + " 条。\n" + sb;
    }
}
