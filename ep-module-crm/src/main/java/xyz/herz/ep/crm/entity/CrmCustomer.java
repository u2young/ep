package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmCustomerStateProxy;
import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
import xyz.herz.ep.crm.handler.CrmCustomerTransferHandler;
import xyz.herz.ep.crm.handler.CrmCustomerClaimHandler;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.expr.ExprBool;
import xyz.erupt.annotation.sub_erupt.Filter;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
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

import lombok.Getter;
import lombok.Setter;

/**
 * 客户。
 * 核心设计:ownerUserId 为 NULL 即公海客户。
 * 公海回收条件(定时任务):ownerUserId != null && lockStatus=0 && dealStatus=0 && 超过 N 天未跟进/未成交。
 * 状态字段:lockStatus(锁定豁免公海),dealStatus(成交豁免公海),followUpStatus(跟进汇总)
 *
 * 注:Erupt 2.0.3 的 Filter.condition → Filter.value(),写 JPQL where 片段。
 * 行按钮可见性由 handler 内做强校验,ExprBool 只提供默认显示。
 *
 * 数据权限:通过 {@link CrmTeamMember} 实现(bizType=1 客户)。P1 仅建实体 + Repository,
 * 未挂 @Filter 行级拦截;后续可在 @Erupt(filter=...) 上扩展当前登录用户的可见范围。
 */
@Getter
@Setter
@Table(name = "crm_customer")
@Entity
@Erupt(
    name = "客户",
    dataProxy = CrmCustomerStateProxy.class,
    power = @Power(importable = true, export = true),
    // 默认视图:"我的客户"。公海客户可以再写一个 CrmCustomerSea 子类或独立 View @Erupt(filter=ownerUserId is null)
    filter = @Filter("ownerUserId is not null"),
    rowOperation = {
        @RowOperation(
            code = "TRANSFER", title = "转移负责人", icon = "fa fa-user-circle-o",
            operationHandler = CrmCustomerTransferHandler.class
        ),
        @RowOperation(
            code = "CLAIM", title = "认领公海客户", icon = "fa fa-hand-paper-o",
            operationHandler = CrmCustomerClaimHandler.class
        ),
        @RowOperation(
            code = "MARK_LOCK", title = "锁定客户", icon = "fa fa-lock",
            show = @ExprBool,
            operationHandler = CrmCustomerTransferHandler.class,
            operationParam = {"MARK_LOCK"}
        ),
        @RowOperation(
            code = "MARK_UNLOCK", title = "解锁客户", icon = "fa fa-unlock",
            show = @ExprBool,
            operationHandler = CrmCustomerTransferHandler.class,
            operationParam = {"MARK_UNLOCK"}
        ),
        @RowOperation(
            code = "MARK_DEAL", title = "标记成交", icon = "fa fa-check-square",
            show = @ExprBool,
            operationHandler = CrmCustomerTransferHandler.class,
            operationParam = {"MARK_DEAL"}
        )
    }
)
public class CrmCustomer extends BaseModel {

    @EruptField(
        views = @View(title = "客户名称", sortable = true),
        edit = @Edit(title = "客户名称", notNull = true, search = @Search)
    )
    private String name;

    // 公海判断核心字段,为 null 即公海
    @EruptField(
        views = @View(title = "负责人ID", sortable = true),
        edit = @Edit(title = "负责人ID", desc = "留空 = 公海客户。认领/转移通过按钮变更,不要直接修改", search = @Search)
    )
    private Long ownerUserId;

    @EruptField(
        views = @View(title = "负责时间", sortable = true),
        edit = @Edit(title = "负责时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME), show = false)
    )
    private LocalDateTime ownerTime;

    // ========== 三状态机:锁定/成交/跟进 ==========
    @EruptField(
        views = @View(title = "锁定状态", sortable = true),
        edit = @Edit(
            title = "锁定状态",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"lock_status"})
        )
    )
    private Integer lockStatus = 0;

    @EruptField(
        views = @View(title = "成交状态", sortable = true),
        edit = @Edit(
            title = "成交状态",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"deal_status"})
        )
    )
    private Integer dealStatus = 0;

    @EruptField(
        views = @View(title = "跟进状态", sortable = true),
        edit = @Edit(
            title = "跟进状态",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"clue_follow_status"})
        )
    )
    private Integer followUpStatus = 0;

    @EruptField(views = @View(title = "最后跟进时间", sortable = true),
        edit = @Edit(title = "最后跟进时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME), show = false))
    private LocalDateTime contactLastTime;

    @EruptField(views = @View(title = "最后跟进内容"),
        edit = @Edit(title = "最后跟进内容", show = false))
    private String contactLastContent;

    @EruptField(views = @View(title = "下次联系时间", sortable = true),
        edit = @Edit(title = "下次联系时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime contactNextTime;

    // ========== 联系方式 ==========
    @EruptField(views = @View(title = "手机号"), edit = @Edit(title = "手机号", search = @Search))
    private String mobile;
    @EruptField(views = @View(title = "座机"), edit = @Edit(title = "座机"))
    private String telephone;
    @EruptField(views = @View(title = "QQ"), edit = @Edit(title = "QQ"))
    private String qq;
    @EruptField(views = @View(title = "微信"), edit = @Edit(title = "微信"))
    private String wechat;
    @EruptField(views = @View(title = "邮箱"), edit = @Edit(title = "邮箱"))
    private String email;

    // ========== 地区 ==========
    @EruptField(views = @View(title = "地区编码"), edit = @Edit(title = "地区编码"))
    private Integer areaId;
    @EruptField(views = @View(title = "详细地址"), edit = @Edit(title = "详细地址"))
    private String detailAddress;

    // ========== 字典 ==========
    @EruptField(
        views = @View(title = "行业"),
        edit = @Edit(title = "行业", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"industry"}))
    )
    private Integer industryId;

    @EruptField(
        views = @View(title = "客户等级"),
        edit = @Edit(title = "客户等级", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"customer_level"}))
    )
    private Integer level;

    @EruptField(
        views = @View(title = "客户来源"),
        edit = @Edit(title = "客户来源", type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"source"}))
    )
    private Integer source;

    // ========== 后续扩展预留字段(合同/回款)MVP 不启用 ==========
    @EruptField(views = @View(title = "合同数(预留)"), edit = @Edit(title = "合同数(预留)", show = false))
    private Integer contractCount = 0;

    @EruptField(views = @View(title = "合同总金额(预留)"), edit = @Edit(title = "合同总金额(预留)", show = false))
    private Long contractAmount = 0L;

    @Lob
    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;
}
