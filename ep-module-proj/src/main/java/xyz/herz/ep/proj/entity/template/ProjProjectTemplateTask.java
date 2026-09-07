package xyz.herz.ep.proj.entity.template;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.MetaModelVo;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 项目模板任务行(参考 ERPNext Project Template Task)。
 * <p>每行:任务标题 + 计划工时;由模板克隆生成项目时,自动创建对应 ProjTask。
 */
@Getter @Setter
@Entity
@Table(name = "proj_project_template_task")
public class ProjProjectTemplateTask extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @EruptField(views = @View(title = "模板"))
    private ProjProjectTemplate template;

    @EruptField(views = @View(title = "任务标题"),
                edit = @Edit(title = "任务标题", notNull = true))
    @Column(name = "subject", length = 200, nullable = false)
    private String subject;

    @EruptField(views = @View(title = "计划工时(小时)"),
                edit = @Edit(title = "计划工时(小时)", numberType = @NumberType(min = 0)))
    @Column(name = "expected_time", precision = 12, scale = 2)
    private BigDecimal expectedTime = BigDecimal.ZERO;
}
