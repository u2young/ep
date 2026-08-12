package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
import xyz.herz.ep.crm.core.CrmClueStateProxy;
import xyz.herz.ep.crm.handler.CrmClueTransformHandler;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
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
 * 线索。
 * 状态机字段:
 *   followUpStatus       (0 未跟进/1 已跟进)
 *   transformStatus      (0 未转化/1 已转化,不可逆,置 1 时写 customerId)
 * 状态字段锁定:禁止在表单直接修改,通过 @RowOperation "转化为客户"触发。
 * 已转化线索:Handler 强校验,按钮是否显式由前端控制。
 */
@Getter
@Setter
@Table(name = "crm_clue")
@Entity
@Erupt(
    name = "线索",
    dataProxy = CrmClueStateProxy.class,
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(
            code = "TRANSFORM", title = "转化为客户", icon = "fa fa-exchange",
            operationHandler = CrmClueTransformHandler.class
        )
    }
)
public class CrmClue extends BaseModel {

    @EruptField(
        views = @View(title = "线索名称", sortable = true),
        edit = @Edit(title = "线索名称", notNull = true, search = @Search)
    )
    private String name;

    @EruptField(
        views = @View(title = "负责人ID"),
        edit = @Edit(title = "负责人ID", desc = "对应用户表 erupt-user 的 id,后续可替换为 ReferenceTable", search = @Search)
    )
    private Long ownerUserId;

    // ========== 跟进状态机 ==========
    @EruptField(
        views = @View(title = "跟进状态", sortable = true),
        edit = @Edit(
            title = "跟进状态",
            type = EditType.CHOICE,
            desc = "请勿直接修改,写跟进会自动变",
            choiceType = @ChoiceType(
                fetchHandler = CrmEnumChoiceFetchHandler.class,
                fetchHandlerParams = {"clue_follow_status"}
            )
        )
    )
    private Integer followUpStatus = 0;

    @EruptField(
        views = @View(title = "最后跟进时间", sortable = true),
        edit = @Edit(title = "最后跟进时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME), show = false)
    )
    private LocalDateTime contactLastTime;

    @EruptField(
        views = @View(title = "最后跟进内容"),
        edit = @Edit(title = "最后跟进内容", show = false)
    )
    private String contactLastContent;

    @EruptField(
        views = @View(title = "下次联系时间", sortable = true),
        edit = @Edit(title = "下次联系时间", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE_TIME))
    )
    private LocalDateTime contactNextTime;

    // ========== 转化状态机 ==========
    @EruptField(
        views = @View(title = "转化状态", sortable = true),
        edit = @Edit(
            title = "转化状态",
            type = EditType.CHOICE,
            desc = "通过【转化为客户】按钮变更",
            choiceType = @ChoiceType(
                fetchHandler = CrmEnumChoiceFetchHandler.class,
                fetchHandlerParams = {"clue_transform_status"}
            )
        )
    )
    private Integer transformStatus = 0;

    @EruptField(
        views = @View(title = "生成客户ID"),
        edit = @Edit(title = "生成客户ID", show = false)
    )
    private Long customerId;

    // ========== 联系方式 ==========
    @EruptField(
        views = @View(title = "手机号"),
        edit = @Edit(title = "手机号", search = @Search)
    )
    private String mobile;

    @EruptField(
        views = @View(title = "座机"),
        edit = @Edit(title = "座机")
    )
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
        edit = @Edit(
            title = "行业",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"industry"})
        )
    )
    private Integer industryId;

    @EruptField(
        views = @View(title = "客户等级"),
        edit = @Edit(
            title = "客户等级",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"customer_level"})
        )
    )
    private Integer level;

    @EruptField(
        views = @View(title = "客户来源"),
        edit = @Edit(
            title = "客户来源",
            type = EditType.CHOICE,
            choiceType = @ChoiceType(fetchHandler = CrmEnumChoiceFetchHandler.class, fetchHandlerParams = {"source"})
        )
    )
    private Integer source;

    @Lob
    @EruptField(
        views = @View(title = "备注"),
        edit = @Edit(title = "备注", type = EditType.TEXTAREA)
    )
    private String remark;
}
