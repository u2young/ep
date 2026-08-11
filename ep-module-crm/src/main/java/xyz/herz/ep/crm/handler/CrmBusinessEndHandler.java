package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmBusiness;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmBusinessRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 商机结束操作:赢单(WIN) / 输单(LOSE) / 无效(INVALID)。
 * 一旦结束,endStatus 不可逆。赢单时,对应客户 deal_status 自动置 1 作为副作用。
 */
@Component
public class CrmBusinessEndHandler implements OperationHandler<CrmBusiness, String> {

    @Resource private CrmBusinessRepository businessRepo;
    @Resource private CrmCustomerRepository customerRepo;

    @Override
    @Transactional
    public String exec(List<CrmBusiness> data, String param, String[] eruptParams) {
        String action = eruptParams == null || eruptParams.length == 0 ? "WIN" : eruptParams[0].toUpperCase();
        int code = switch (action) {
            case "WIN" -> CrmDictEnums.BusinessEndStatus.WON.code;
            case "LOSE" -> CrmDictEnums.BusinessEndStatus.LOST.code;
            case "INVALID" -> CrmDictEnums.BusinessEndStatus.INVALID.code;
            default -> throw new IllegalArgumentException("未知结束动作: " + action);
        };
        int ok = 0; StringBuilder sb = new StringBuilder();
        for (CrmBusiness b : data) {
            try {
                if (b.getEndStatus() != null) {
                    sb.append("商机[").append(b.getName()).append("] 已结束,不可重复设置\n");
                    continue;
                }
                b.setEndStatus(code);
                if (param != null && !param.isBlank()) b.setEndRemark(param);
                else if (b.getEndRemark() == null) b.setEndRemark(action);
                businessRepo.save(b);
                // 赢单副作用:对应客户自动标记成交
                if (code == CrmDictEnums.BusinessEndStatus.WON.code && b.getCustomerId() != null) {
                    customerRepo.findById(b.getCustomerId()).ifPresent(c -> {
                        if (c.getDealStatus() == null || c.getDealStatus() == 0) {
                            c.setDealStatus(CrmDictEnums.DealStatus.DEALT.code);
                            customerRepo.save(c);
                        }
                    });
                }
                ok++;
            } catch (Exception e) {
                sb.append("商机[").append(b.getName()).append("] 失败:").append(e.getMessage()).append('\n');
            }
        }
        return "动作[" + action + "]完成:成功 " + ok + " 条。\n" + sb;
    }
}
