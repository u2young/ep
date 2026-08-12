package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmContractRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 合同【生效】按钮:status 0(草稿) → 1(生效)。
 *
 * <p>强校验前置状态:仅 status=0 草稿可生效。已生效 / 已作废一律拒绝。
 * 生效后不可回退到草稿,只能继续作废。
 */
@Component
public class CrmContractEffectHandler implements OperationHandler<CrmContract, String> {

    @Resource private CrmContractRepository contractRepo;

    @Override
    @Transactional
    public String exec(List<CrmContract> data, String param, String[] eruptParams) {
        int ok = 0;
        StringBuilder sb = new StringBuilder();
        for (CrmContract c : data) {
            try {
                Integer s = c.getStatus();
                if (s == null) s = 0;
                if (s != CrmDictEnums.ContractStatus.DRAFT.code) {
                    sb.append("合同[").append(c.getNo())
                        .append("] 当前状态=").append(s).append(",仅草稿(0)可生效\n");
                    continue;
                }
                c.setStatus(CrmDictEnums.ContractStatus.EFFECTIVE.code);
                contractRepo.save(c);
                ok++;
            } catch (Exception e) {
                sb.append("合同[").append(c == null ? "-" : c.getNo())
                    .append("] 生效失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "合同生效:成功 " + ok + " 条。\n" + sb;
    }
}
