package xyz.herz.ep.sup.entity.settings;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;

import jakarta.persistence.*;

/**
 * 服务支持单例配置(参考 ERPNext Support Settings)。
 * <p>全局唯一配置:{@code defaultSlaId} 默认 SLA、{@code defaultAgentId} 默认客服、
 * {@code autoAssignEnabled} 自动分派开关、{@code kbPublicVisible} 知识库是否对匿名可见。
 *
 * <p>单例约定:由 {@link xyz.herz.ep.sup.handler.settings.SupSupportSettingsInitializer} 在表空时插入 1 条默认记录,
 * 业务方通过 {@code SupSupportSettingsRepository.findFirstByOrderByIdAsc()} 取单例。
 */
@Getter @Setter
@Entity
@Table(name = "sup_settings")
@Erupt(name = "服务支持配置(单例)")
public class SupSupportSettings extends MetaModelVo {

    @EruptField(views = @View(title = "默认 SLA ID"), edit = @Edit(title = "默认 SLA ID"))
    @Column(name = "default_sla_id")
    private Long defaultSlaId;

    @EruptField(views = @View(title = "默认客服ID"), edit = @Edit(title = "默认客服ID"))
    @Column(name = "default_agent_id")
    private Long defaultAgentId;

    @EruptField(views = @View(title = "默认客服"), edit = @Edit(title = "默认客服"))
    @Column(length = 100)
    private String defaultAgentName;

    @EruptField(views = @View(title = "自动分派"),
                edit = @Edit(title = "自动分派", notNull = true))
    @Column(name = "auto_assign_enabled", nullable = false)
    private Boolean autoAssignEnabled = Boolean.FALSE;

    @EruptField(views = @View(title = "知识库公开可见"),
                edit = @Edit(title = "知识库公开可见", notNull = true))
    @Column(name = "kb_public_visible", nullable = false)
    private Boolean kbPublicVisible = Boolean.FALSE;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
