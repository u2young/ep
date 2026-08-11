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
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 联系人。MVP 暂不做 parent_id 树形(联系人上级)和 master 决策人过滤,先放字段留扩展。
 */
@Table(name = "crm_contact")
@Entity
@Erupt(name = "联系人", power = @Power(importable = true, export = true))
public class CrmContact extends BaseModel {

    @EruptField(views = @View(title = "姓名", sortable = true),
        edit = @Edit(title = "姓名", notNull = true, search = @Search))
    private String name;

    @EruptField(views = @View(title = "关联客户ID"),
        edit = @Edit(title = "关联客户ID", notNull = true, search = @Search,
            desc = "对应 crm_customer.id"))
    private Long customerId;

    @EruptField(views = @View(title = "上级联系人ID(预留)"),
        edit = @Edit(title = "上级联系人ID(预留)", desc = "树形,暂不启用"))
    private Long parentId;

    @EruptField(views = @View(title = "关键决策人"),
        edit = @Edit(title = "关键决策人"))
    private Boolean master = Boolean.FALSE;

    @EruptField(views = @View(title = "职位"), edit = @Edit(title = "职位"))
    private String post;

    @EruptField(views = @View(title = "性别"),
        edit = @Edit(title = "性别", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"sex"})))
    private Integer sex;

    @EruptField(views = @View(title = "手机号"), edit = @Edit(title = "手机号", search = @Search))
    private String mobile;
    @EruptField(views = @View(title = "座机"), edit = @Edit(title = "座机"))
    private String telephone;
    @EruptField(views = @View(title = "邮箱"), edit = @Edit(title = "邮箱"))
    private String email;
    @EruptField(views = @View(title = "QQ"), edit = @Edit(title = "QQ"))
    private String qq;
    @EruptField(views = @View(title = "微信"), edit = @Edit(title = "微信"))
    private String wechat;

    @EruptField(views = @View(title = "地区编码"), edit = @Edit(title = "地区编码"))
    private Integer areaId;
    @EruptField(views = @View(title = "详细地址"), edit = @Edit(title = "详细地址"))
    private String detailAddress;

    @EruptField(views = @View(title = "负责人ID"), edit = @Edit(title = "负责人ID"))
    private Long ownerUserId;

    @EruptField(views = @View(title = "跟进状态"),
        edit = @Edit(title = "跟进状态", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"clue_follow_status"})))
    private Integer followUpStatus = 0;

    @EruptField(views = @View(title = "最后跟进时间"),
        edit = @Edit(title = "最后跟进时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME), show = false))
    private LocalDateTime contactLastTime;

    @EruptField(views = @View(title = "最后跟进内容"),
        edit = @Edit(title = "最后跟进内容", show = false))
    private String contactLastContent;

    @EruptField(views = @View(title = "下次联系时间"),
        edit = @Edit(title = "下次联系时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime contactNextTime;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;

    // getter/setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Boolean getMaster() { return master; }
    public void setMaster(Boolean master) { this.master = master; }
    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public Integer getAreaId() { return areaId; }
    public void setAreaId(Integer areaId) { this.areaId = areaId; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Integer getFollowUpStatus() { return followUpStatus; }
    public void setFollowUpStatus(Integer followUpStatus) { this.followUpStatus = followUpStatus; }
    public LocalDateTime getContactLastTime() { return contactLastTime; }
    public void setContactLastTime(LocalDateTime contactLastTime) { this.contactLastTime = contactLastTime; }
    public String getContactLastContent() { return contactLastContent; }
    public void setContactLastContent(String contactLastContent) { this.contactLastContent = contactLastContent; }
    public LocalDateTime getContactNextTime() { return contactNextTime; }
    public void setContactNextTime(LocalDateTime contactNextTime) { this.contactNextTime = contactNextTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
