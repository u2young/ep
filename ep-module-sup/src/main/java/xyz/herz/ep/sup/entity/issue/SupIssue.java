package xyz.herz.ep.sup.entity.issue;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.fun.EruptButtonHandler;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.sup.core.SupEnumChoiceFetchHandler;
import xyz.herz.ep.sup.core.SupStateDataProxy;
import xyz.herz.ep.sup.entity.assignment.SupIssueAssignment;
import xyz.herz.ep.sup.entity.sla.SupServiceLevelAgreement;
import xyz.herz.ep.sup.enums.SupDictEnums.IssuePriority;
import xyz.herz.ep.sup.enums.SupDictEnums.IssueStatus;
import xyz.herz.ep.sup.handler.issue.SupIssueAutoAssignButtonHandler;
import xyz.herz.ep.sup.handler.issue.SupIssueLifecycleHandler;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户工单(参考 ERPNext Issue DocType)。
 * <p>状态机:0打开 → 1已回复 → 2已解决 → 3已关闭;支持 4重新打开 → 回到打开循环;5取消终态。
 * <p>SLA 引擎:SLA 字段关联 {@link SupServiceLevelAgreement},
 * {@code responseBy/resolutionBy} 由 SupSlaEvaluator 计算;{@code slaFulfilled} 为派生字段。
 *
 * <p>跨模块集成(I-12):customer REF {@link CrmCustomer}(optional,nullable=true,不强制 FK);
 * assignee 用快照(assigneeId+assigneeName),不 REF UPMS 用户实体。
 */
@Getter @Setter
@Entity
@Table(name = "sup_issue")
@Erupt(
    name = "客户工单",
    power = @Power(importable = true, export = true),
    dataProxy = SupIssue.Proxy.class,
    rowOperation = {
        @RowOperation(title = "回复", code = SupIssueLifecycleHandler.CODE_REPLY,
            operationHandler = SupIssueLifecycleHandler.class,
            operationParam = { SupIssueLifecycleHandler.CODE_REPLY }),
        @RowOperation(title = "解决", code = SupIssueLifecycleHandler.CODE_RESOLVE,
            operationHandler = SupIssueLifecycleHandler.class,
            operationParam = { SupIssueLifecycleHandler.CODE_RESOLVE }),
        @RowOperation(title = "关闭", code = SupIssueLifecycleHandler.CODE_CLOSE,
            operationHandler = SupIssueLifecycleHandler.class,
            operationParam = { SupIssueLifecycleHandler.CODE_CLOSE }),
        @RowOperation(title = "重开", code = SupIssueLifecycleHandler.CODE_REOPEN,
            operationHandler = SupIssueLifecycleHandler.class,
            operationParam = { SupIssueLifecycleHandler.CODE_REOPEN }),
        @RowOperation(title = "取消", code = SupIssueLifecycleHandler.CODE_CANCEL,
            operationHandler = SupIssueLifecycleHandler.class,
            operationParam = { SupIssueLifecycleHandler.CODE_CANCEL })
    }
)
public class SupIssue extends MetaModelVo {

    @EruptField(views = @View(title = "工单主题"),
                edit = @Edit(title = "工单主题", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String subject;

    // I-12 跨模块 REF:客户(optional,nullable=true,不强制 FK,避免 H2 create-drop fixture 顺序耦合)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id", nullable = true)
    @EruptField(views = @View(title = "客户", column = "name"),
                edit = @Edit(title = "客户", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private CrmCustomer customer;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "IssueStatus")))
    @Column(nullable = false)
    private Integer status = IssueStatus.OPEN.code;

    @EruptField(views = @View(title = "优先级"),
                edit = @Edit(title = "优先级", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "IssuePriority")))
    @Column(nullable = false)
    private Integer priority = IssuePriority.MEDIUM.code;

    @EruptField(views = @View(title = "提交人"), edit = @Edit(title = "提交人"))
    @Column(length = 100)
    private String raisedBy;

    @EruptField(views = @View(title = "提交人ID"), edit = @Edit(title = "提交人ID", show = false))
    @Column(name = "raised_by_id")
    private Long raisedById;

    @EruptField(views = @View(title = "分派给"), edit = @Edit(title = "分派给"))
    @Column(length = 100)
    private String assigneeName;

    @EruptField(views = @View(title = "分派人ID"), edit = @Edit(title = "分派人ID", show = false))
    @Column(name = "assignee_id")
    private Long assigneeId;

    // SLA 关联(optional:工单可无 SLA,如内部工单)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sla_id", nullable = true)
    @EruptField(views = @View(title = "SLA", column = "name"),
                edit = @Edit(title = "SLA", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private SupServiceLevelAgreement sla;

    @EruptField(views = @View(title = "响应截止"), edit = @Edit(title = "响应截止", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "response_by")
    private LocalDateTime responseBy;

    @EruptField(views = @View(title = "解决截止"), edit = @Edit(title = "解决截止", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "resolution_by")
    private LocalDateTime resolutionBy;

    @EruptField(views = @View(title = "首次响应"), edit = @Edit(title = "首次响应", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @EruptField(views = @View(title = "解决时间"), edit = @Edit(title = "解决时间", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @EruptField(views = @View(title = "关闭时间"), edit = @Edit(title = "关闭时间", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @EruptField(views = @View(title = "SLA 达成"), edit = @Edit(title = "SLA 达成", show = false))
    @Column(name = "sla_fulfilled")
    private Boolean slaFulfilled;

    @EruptField(views = @View(title = "描述"), edit = @Edit(title = "描述", type = EditType.TEXTAREA))
    @Lob
    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    // =================== AutoAssign 表内按钮(根据 SupSupportSettings 自动分派) ===================
    @Transient
    @EruptField(
        views = @View(title = "自动分派触发器", show = false),
        edit = @Edit(
            title = "自动分派（按 SupSupportSettings 默认客服分派）",
            type = EditType.BUTTON,
            desc = "点按钮根据服务支持设置(SupSupportSettings)的默认客服自动分派工单",
            buttonType = @ButtonType(
                handler = SupIssueAutoAssignButtonHandler.class,
                icon = "fa fa-user-plus",
                confirm = "根据服务支持设置的默认客服自动分派本工单,确认继续?",
                style = "primary"
            )
        )
    )
    private Integer runAutoAssignTrigger = 1;

    // =================== 分派历史子表 ===================
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "分派历史", type = EditType.TAB_TABLE_ADD))
    private List<SupIssueAssignment> assignments = new ArrayList<>();

    public static class Proxy extends SupStateDataProxy<SupIssue> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
