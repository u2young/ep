package xyz.herz.ep.crm.jpa;

import xyz.herz.ep.crm.entity.CrmTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmTeamMemberRepository extends JpaRepository<CrmTeamMember, Long> {

    /** 按业务类型 + 业务ID 查询该业务对象下的所有团队成员。 */
    List<CrmTeamMember> findByBizTypeAndBizId(Integer bizType, Long bizId);
}
