package xyz.herz.ep.sup.entity.sla;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.sup.core.SupEnumChoiceFetchHandler;
import xyz.herz.ep.sup.core.SupStateDataProxy;
import xyz.herz.ep.sup.enums.SupDictEnums.EnableStatus;
import xyz.herz.ep.sup.enums.SupDictEnums.IssuePriority;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * SLA 服务等级协议(参考 ERPNext Service Level Agreement)。
 * <p>customer=null 表示全公司默认 SLA;非空表示该客户专属 SLA。
 * <p>responseTimeMins/resolutionTimeMins:从工单创建时间起算的响应/解决时长(分钟)。
 * SupSlaEvaluator 用此计算 {@link xyz.herz.ep.sup.entity.issue.SupIssue#getResponseBy()} 截止。
 */
@Getter @Setter
@Entity
@Table(name = "sup_sla")
@Erupt(
    name = "SLA 服务等级协议",
    power = @Power(importable = true, export = true),
    dataProxy = SupServiceLevelAgreement.Proxy.class
)
public class SupServiceLevelAgreement extends MetaModelVo {

    @EruptField(views = @View(title = "SLA 名称"),
                edit = @Edit(title = "SLA 名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    // 客户 REF(optional:null=全公司默认 SLA)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "customer_id", nullable = true)
    @EruptField(views = @View(title = "客户", column = "name"),
                edit = @Edit(title = "客户(空=全公司)", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private CrmCustomer customer;

    @EruptField(views = @View(title = "适用优先级"),
                edit = @Edit(title = "适用优先级", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "IssuePriority")))
    @Column(name = "priority", nullable = false)
    private Integer priority = IssuePriority.MEDIUM.code;

    @EruptField(views = @View(title = "响应时长(分钟)"),
                edit = @Edit(title = "响应时长(分钟)", notNull = true))
    @Column(name = "response_time_mins", nullable = false)
    private Integer responseTimeMins = 60;

    @EruptField(views = @View(title = "解决时长(分钟)"),
                edit = @Edit(title = "解决时长(分钟)", notNull = true))
    @Column(name = "resolution_time_mins", nullable = false)
    private Integer resolutionTimeMins = 480;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "生效日期"),
                edit = @Edit(title = "生效日期", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "start_date")
    private LocalDate startDate;

    @EruptField(views = @View(title = "失效日期"),
                edit = @Edit(title = "失效日期", dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "end_date")
    private LocalDate endDate;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SupStateDataProxy<SupServiceLevelAgreement> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
