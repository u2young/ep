package xyz.herz.ep.crm.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 团队成员(数据权限 P1 扩展)。
 *
 * <p>通过 {@code bizType + bizId} 关联到具体的客户/商机/联系人,每个业务对象可以有多个团队成员,
 * 其中一个角色为「负责人」(role=1),其余为「跟进人」(role=2) 或「只读」(role=3)。
 *
 * <p>P1 阶段只建实体 + Repository,未挂 @Filter 拦截器;后续 P2 可在 CrmCustomer/CrmBusiness
 * 上加 Erupt 行级过滤,基于本表 + 当前登录用户 ID 决定可见范围。
 *
 * <p>level 字段为权限级别预留:1本人 2本部门 3本部门及下属 4全部。P1 不做强制校验。
 */
@Getter
@Setter
@Table(name = "crm_team_member")
@Entity
@Erupt(name = "团队成员", power = @Power(export = true))
public class CrmTeamMember extends BaseModel {

    @EruptField(
        views = @View(title = "业务类型", sortable = true),
        edit = @Edit(title = "业务类型", notNull = true, type = EditType.CHOICE, search = @Search,
            desc = "1客户 2商机 3联系人",
            choiceType = @ChoiceType(vl = {
                @VL(value = "1", label = "客户"),
                @VL(value = "2", label = "商机"),
                @VL(value = "3", label = "联系人")
            }))
    )
    private Integer bizType;

    @EruptField(
        views = @View(title = "业务ID"),
        edit = @Edit(title = "业务ID", notNull = true, search = @Search,
            desc = "对应 crm_customer / crm_business / crm_contact 的主键 id")
    )
    private Long bizId;

    @EruptField(
        views = @View(title = "用户ID", sortable = true),
        edit = @Edit(title = "用户ID", notNull = true, search = @Search,
            desc = "erupt-user.id")
    )
    private Long userId;

    @EruptField(
        views = @View(title = "角色", sortable = true),
        edit = @Edit(title = "角色", notNull = true, type = EditType.CHOICE,
            desc = "1负责人 2跟进人 3只读",
            choiceType = @ChoiceType(vl = {
                @VL(value = "1", label = "负责人"),
                @VL(value = "2", label = "跟进人"),
                @VL(value = "3", label = "只读")
            }))
    )
    private Integer role;

    @EruptField(
        views = @View(title = "权限级别", sortable = true),
        edit = @Edit(title = "权限级别", type = EditType.CHOICE,
            desc = "1本人 2本部门 3本部门及下属 4全部",
            choiceType = @ChoiceType(vl = {
                @VL(value = "1", label = "本人"),
                @VL(value = "2", label = "本部门"),
                @VL(value = "3", label = "本部门及下属"),
                @VL(value = "4", label = "全部")
            }))
    )
    private Integer level;
}
