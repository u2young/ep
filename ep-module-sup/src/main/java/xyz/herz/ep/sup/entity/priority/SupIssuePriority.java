package xyz.herz.ep.sup.entity.priority;

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
import xyz.herz.ep.sup.core.SupEnumChoiceFetchHandler;
import xyz.herz.ep.sup.core.SupStateDataProxy;
import xyz.herz.ep.sup.enums.SupDictEnums.EnableStatus;

import jakarta.persistence.*;

/**
 * 工单优先级主数据(参考 ERPNext Issue Priority)。
 * <p>预置低/中/高/紧急四级;每级配默认 SLA 响应/解决时长(分钟),
 * 新建工单未指定 SLA 时 SupSlaEvaluator 用此默认值计算截止。
 */
@Getter @Setter
@Entity
@Table(name = "sup_issue_priority")
@Erupt(
    name = "工单优先级",
    power = @Power(importable = true, export = true),
    dataProxy = SupIssuePriority.Proxy.class
)
public class SupIssuePriority extends MetaModelVo {

    @EruptField(views = @View(title = "编码"),
                edit = @Edit(title = "编码", notNull = true, search = @Search))
    @Column(length = 30, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "名称"), edit = @Edit(title = "名称", notNull = true))
    @Column(length = 60, nullable = false)
    private String name;

    @EruptField(views = @View(title = "默认响应时长(分钟)"),
                edit = @Edit(title = "默认响应时长(分钟)", notNull = true))
    @Column(name = "default_sla_response_mins", nullable = false)
    private Integer defaultSlaResponseMins = 60;

    @EruptField(views = @View(title = "默认解决时长(分钟)"),
                edit = @Edit(title = "默认解决时长(分钟)", notNull = true))
    @Column(name = "default_sla_resolution_mins", nullable = false)
    private Integer defaultSlaResolutionMins = 480;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SupStateDataProxy<SupIssuePriority> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
