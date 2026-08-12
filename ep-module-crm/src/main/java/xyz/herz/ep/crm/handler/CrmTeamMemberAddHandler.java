package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmTeamMember;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.jpa.CrmTeamMemberRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 团队成员「添加成员」行按钮(可选,P1 仅占位)。
 *
 * <p>使用方式:在 CrmCustomer/CrmBusiness 行上配置 {@code @RowOperation(code="ADD_TEAM_MEMBER",
 * operationHandler = CrmTeamMemberAddHandler.class, operationParam = {"1"} // 1=客户})},
 * 弹出 erupt 表单录入 userId/role/level,执行后写入 crm_team_member。
 *
 * <p>P1 MVP 不强校验「一个业务对象只能有一个负责人」,只做基本写入。
 */
@Component
public class CrmTeamMemberAddHandler implements OperationHandler<CrmTeamMember, String> {

    @Resource private CrmTeamMemberRepository teamRepo;

    @Override
    @Transactional
    public String exec(List<CrmTeamMember> data, String param, String[] eruptParams) {
        // eruptParams[0] = bizType 字符串,由 @RowOperation operationParam 提供
        Integer bizType = null;
        if (eruptParams != null && eruptParams.length > 0) {
            bizType = Integer.valueOf(eruptParams[0]);
        }
        int ok = 0;
        StringBuilder sb = new StringBuilder();
        for (CrmTeamMember m : data) {
            try {
                if (bizType != null) m.setBizType(bizType);
                if (m.getRole() == null) m.setRole(CrmDictEnums.TeamRole.FOLLOWER.code);
                if (m.getLevel() == null) m.setLevel(1);
                teamRepo.save(m);
                ok++;
            } catch (Exception e) {
                sb.append("添加失败: ").append(e.getMessage()).append('\n');
            }
        }
        return "添加团队成员:成功 " + ok + " 条。\n" + sb;
    }
}
