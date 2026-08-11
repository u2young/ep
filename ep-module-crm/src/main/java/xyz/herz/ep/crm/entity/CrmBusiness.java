package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmBusinessStateProxy;
import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
import xyz.herz.ep.crm.handler.CrmBusinessAdvanceHandler;
import xyz.herz.ep.crm.handler.CrmBusinessEndHandler;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.expr.ExprBool;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商机。三层状态机:
 *   status_type_id            所属状态组
 *   status_id                 当前所处阶段(同组内推进/退回用 RowOperation)
 *   end_status                结束状态(null=进行中 / 1赢单 / 2输单 / 3无效,不可逆)
 *
 * 赢单通常触发:成交 deal_status=1、可创建合同;输单/无效关闭该商机不影响客户。
 */
@Table(name = "crm_business")
@Entity
@Erupt(
    name = "商机",
    dataProxy = CrmBusinessStateProxy.class,
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(
            code = "ADVANCE", title = "推进下一阶段", icon = "fa fa-arrow-right",
            operationHandler = CrmBusinessAdvanceHandler.class,
            operationParam = {"FORWARD"}
        ),
        @RowOperation(
            code = "BACK", title = "回退上一阶段", icon = "fa fa-arrow-left",
            operationHandler = CrmBusinessAdvanceHandler.class,
            operationParam = {"BACK"}
        ),
        @RowOperation(
            code = "WIN", title = "赢单", icon = "fa fa-trophy",
            show = @ExprBool(value = true),  // 仅 end_status == null 时才显示需 exprHandler 实现,MVP 先允许任意,Service 内强校验
            operationHandler = CrmBusinessEndHandler.class,
            operationParam = {"WIN"}
        ),
        @RowOperation(
            code = "LOSE", title = "输单", icon = "fa fa-times",
            operationHandler = CrmBusinessEndHandler.class,
            operationParam = {"LOSE"}
        ),
        @RowOperation(
            code = "INVALID", title = "无效", icon = "fa fa-ban",
            operationHandler = CrmBusinessEndHandler.class,
            operationParam = {"INVALID"}
        )
    }
)
public class CrmBusiness extends BaseModel {

    @EruptField(views = @View(title = "商机名称", sortable = true),
        edit = @Edit(title = "商机名称", notNull = true, search = @Search))
    private String name;

    @EruptField(views = @View(title = "客户ID"), edit = @Edit(title = "客户ID", notNull = true, search = @Search, desc = "crm_customer.id"))
    private Long customerId;

    @EruptField(views = @View(title = "负责人ID"), edit = @Edit(title = "负责人ID", search = @Search))
    private Long ownerUserId;

    // ========== 跟进汇总 ==========
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

    // ========== 三层状态机 ==========
    @EruptField(views = @View(title = "状态组ID", sortable = true),
        edit = @Edit(title = "状态组ID", notNull = true, desc = "crm_business_status_type.id"))
    private Long statusTypeId;

    @EruptField(views = @View(title = "当前阶段ID", sortable = true),
        edit = @Edit(title = "当前阶段ID", desc = "通过【推进/回退】按钮变更,不要直接修改"))
    private Long statusId;

    @EruptField(views = @View(title = "结束状态"),
        edit = @Edit(title = "结束状态", type = EditType.CHOICE,
            desc = "空=进行中;赢单/输单/无效通过行按钮设置,设置后不可逆",
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"business_end_status"})))
    private Integer endStatus;

    @EruptField(views = @View(title = "结束备注"),
        edit = @Edit(title = "结束备注", desc = "赢单/输单/无效时的原因", type = EditType.TEXTAREA, show = false))
    private String endRemark;

    @EruptField(views = @View(title = "预计成交日期", sortable = true),
        edit = @Edit(title = "预计成交日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime dealTime;

    // ========== 金额 ==========
    @EruptField(views = @View(title = "产品总金额", sortable = true),
        edit = @Edit(title = "产品总金额", type = EditType.NUMBER))
    private Long totalProductPrice = 0L;

    @EruptField(views = @View(title = "整单折扣%"),
        edit = @Edit(title = "整单折扣%", type = EditType.NUMBER,
            numberType = @NumberType(max = 100, min = 0),
            desc = "如 90=9折,100=不打折"))
    private BigDecimal discountPercent;

    @EruptField(views = @View(title = "商机总金额", sortable = true),
        edit = @Edit(title = "商机总金额", type = EditType.NUMBER,
            desc = "产品总金额 × (折扣÷100)。保存时通过 DataProxy 自动重算"))
    private Long totalPrice = 0L;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;

    // getter/setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
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
    public Long getStatusTypeId() { return statusTypeId; }
    public void setStatusTypeId(Long statusTypeId) { this.statusTypeId = statusTypeId; }
    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }
    public Integer getEndStatus() { return endStatus; }
    public void setEndStatus(Integer endStatus) { this.endStatus = endStatus; }
    public String getEndRemark() { return endRemark; }
    public void setEndRemark(String endRemark) { this.endRemark = endRemark; }
    public LocalDateTime getDealTime() { return dealTime; }
    public void setDealTime(LocalDateTime dealTime) { this.dealTime = dealTime; }
    public Long getTotalProductPrice() { return totalProductPrice; }
    public void setTotalProductPrice(Long totalProductPrice) { this.totalProductPrice = totalProductPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Long totalPrice) { this.totalPrice = totalPrice; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
