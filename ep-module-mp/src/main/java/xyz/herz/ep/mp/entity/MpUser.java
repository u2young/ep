package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler;
import xyz.herz.ep.mp.enums.MpDictEnums.SubscribeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 粉丝。subscribeStatus 为状态机字段(关注/取关)。 */
@Getter @Setter
@Entity
@Table(name = "mp_user")
@Erupt(name = "粉丝管理", power = @Power(importable = true, export = true))
public class MpUser extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "OpenID"),
                edit = @Edit(title = "OpenID", notNull = true, search = @Search))
    @Column(length = 64, nullable = false, unique = true)
    private String openId;

    @EruptField(views = @View(title = "昵称"), edit = @Edit(title = "昵称"))
    @Column(length = 64)
    private String nickname;

    @EruptField(views = @View(title = "性别"), edit = @Edit(title = "性别", desc = "1男 2女 0未知"))
    private Integer sex;

    @EruptField(views = @View(title = "城市"), edit = @Edit(title = "城市"))
    @Column(length = 64)
    private String city;

    @EruptField(views = @View(title = "关注状态", sortable = true),
                edit = @Edit(title = "关注状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "SubscribeStatus")))
    private Integer subscribeStatus = SubscribeStatus.UNSUBSCRIBED.code;

    @EruptField(views = @View(title = "关注时间", sortable = true),
                edit = @Edit(title = "关注时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime subscribeTime;

    @EruptField(views = @View(title = "取关时间", sortable = true),
                edit = @Edit(title = "取关时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime unsubscribeTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
