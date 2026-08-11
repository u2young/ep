package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmCustomerPoolConfigRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户行操作统一入口:转移负责人 / 认领公海 / 锁定 / 解锁 / 标记成交。
 * 通过 operationParam 区分动作:
 *  - transfer: eruptionClass 参数为 "TRANSFER" 走转移
 *  - claim:    operationParam[0]="CLAIM"
 *  - MARK_LOCK / MARK_UNLOCK / MARK_DEAL
 */
@Component
public class CrmCustomerTransferHandler implements OperationHandler<CrmCustomer, String> {

    @Resource private CrmCustomerRepository customerRepo;
    @Resource private CrmCustomerPoolConfigRepository poolRepo;

    @Override
    @Transactional
    public String exec(List<CrmCustomer> data, String param, String[] eruptParams) {
        // erupt 2.0.3 单个 @RowOperation 的 operationParam 是数组,判断第一个值
        String action = eruptParams == null || eruptParams.length == 0 ? "TRANSFER" : eruptParams[0];
        int ok = 0; StringBuilder errors = new StringBuilder();
        for (CrmCustomer c : data) {
            try {
                apply(c, action, param);
                customerRepo.save(c);
                ok++;
            } catch (Exception e) {
                errors.append(c.getName()).append(" 失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "动作[" + action + "]:成功 " + ok + " 条,失败 " + (data.size() - ok) + " 条。\n" + errors;
    }

    private void apply(CrmCustomer c, String action, String paramNewOwnerStr) {
        switch (action) {
            case "TRANSFER" -> {
                // 转移负责人:param 里给新负责人 id(字符串);无就置为公海
                if (paramNewOwnerStr == null || paramNewOwnerStr.isBlank()) {
                    c.setOwnerUserId(null);
                    c.setOwnerTime(null);
                } else {
                    Long newOwner = Long.valueOf(paramNewOwnerStr.trim());
                    c.setOwnerUserId(newOwner);
                    c.setOwnerTime(LocalDateTime.now());
                }
            }
            case "CLAIM" -> {
                if (c.getOwnerUserId() != null) throw new IllegalStateException("非公海客户,无法认领");
                checkLimit(null); // todo:当前登录人先传 null,MVP 先不过人数上限
                Long me = currentUserId();
                c.setOwnerUserId(me);
                c.setOwnerTime(LocalDateTime.now());
            }
            case "MARK_LOCK" -> {
                c.setLockStatus(CrmDictEnums.LockStatus.LOCKED.code);
                checkLimit(currentUserId());
            }
            case "MARK_UNLOCK" -> c.setLockStatus(CrmDictEnums.LockStatus.NORMAL.code);
            case "MARK_DEAL" -> c.setDealStatus(CrmDictEnums.DealStatus.DEALT.code);
            default -> throw new IllegalArgumentException("未知 action: " + action);
        }
    }

    private Long currentUserId() {
        // erupt 的登录上下文可以通过 EruptRequestHolder.getUID() 拿;MVP 临时给固定 1
        try {
            Object uid = Class.forName("xyz.erupt.core.context.EruptRequestHolder")
                .getMethod("getUID").invoke(null);
            if (uid != null) return Long.valueOf(String.valueOf(uid));
        } catch (Exception ignore) {}
        return 1L;
    }

    private void checkLimit(Long ownerUserId) {
        if (ownerUserId == null) return;
        poolRepo.findAll().stream().findFirst().ifPresent(cfg -> {
            if (cfg.getReceiveOwnerCount() != null && cfg.getReceiveOwnerCount() > 0) {
                long count = customerRepo.countByOwnerUserId(ownerUserId);
                if (count >= cfg.getReceiveOwnerCount()) {
                    throw new IllegalStateException("该销售人员拥有客户数已达上限(" + cfg.getReceiveOwnerCount() + "),请先释放部分客户。");
                }
            }
        });
    }
}
