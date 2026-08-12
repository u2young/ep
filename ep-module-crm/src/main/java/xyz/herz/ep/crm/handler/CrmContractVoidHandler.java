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
 * 合同【作废】按钮:status 0(草稿) / 1(生效) → 2(作废)。
 *
 * <p>强校验前置状态:仅 status=0 草稿 或 status=1 生效 可作废。已作废(2)一律拒绝。
 * 作废后不可逆。
 */
@Component
public class CrmContractVoidHandler implements OperationHandler<CrmContract, String> {

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
                if (s == CrmDictEnums.ContractStatus.VOID.code) {
                    sb.append("合同[").append(c.getNo()).append("] 已作废,不可重复作废\n");
                    continue;
                }
                c.setStatus(CrmDictEnums.ContractStatus.VOID.code);
                contractRepo.save(c);
                ok++;
            } catch (Exception e) {
                sb.append("合同[").append(c == null ? "-" : c.getNo())
                    .append("] 作废失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "合同作废:成功 " + ok + " 条。\n" + sb;
    }
}
