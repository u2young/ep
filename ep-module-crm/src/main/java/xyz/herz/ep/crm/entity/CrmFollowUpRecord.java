package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
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

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 跟进记录(通用)。
 * 通过 biz_type + biz_id 关联线索/客户/联系人/商机。
 * 新建记录时 CrmFollowService 会同步刷新对应主体的 followUpStatus / contactLastTime / contactLastContent。
 */
@Table(name = "crm_follow_up_record")
@Entity
@Erupt(name = "跟进记录", power = @Power(export = true, add = false, edit = false))
public class CrmFollowUpRecord extends BaseModel {

    @EruptField(views = @View(title = "对象类型", sortable = true),
        edit = @Edit(title = "对象类型", type = EditType.CHOICE, notNull = true, search = @Search,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"follow_biz_type"})))
    private Integer bizType;

    @EruptField(views = @View(title = "对象ID"),
        edit = @Edit(title = "对象ID", notNull = true, search = @Search,
            desc = "线索/客户/联系人/商机的主键 id"))
    private Long bizId;

    @EruptField(views = @View(title = "跟进方式", sortable = true),
        edit = @Edit(title = "跟进方式", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"follow_way"})))
    private Integer followWay;

    @EruptField(views = @View(title = "跟进时间", sortable = true),
        edit = @Edit(title = "跟进时间", type = EditType.DATE,
            dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime followTime;

    @Lob
    @EruptField(views = @View(title = "跟进内容"), edit = @Edit(title = "跟进内容", type = EditType.TEXTAREA, notNull = true))
    private String content;

    @EruptField(views = @View(title = "下次联系时间"),
        edit = @Edit(title = "下次联系时间", type = EditType.DATE,
            dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime contactNextTime;

    // getter/setter
    public Integer getBizType() { return bizType; }
    public void setBizType(Integer bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public Integer getFollowWay() { return followWay; }
    public void setFollowWay(Integer followWay) { this.followWay = followWay; }
    public LocalDateTime getFollowTime() { return followTime; }
    public void setFollowTime(LocalDateTime followTime) { this.followTime = followTime; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getContactNextTime() { return contactNextTime; }
    public void setContactNextTime(LocalDateTime contactNextTime) { this.contactNextTime = contactNextTime; }
}
