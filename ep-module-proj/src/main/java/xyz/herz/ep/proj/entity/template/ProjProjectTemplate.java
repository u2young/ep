package xyz.herz.ep.proj.entity.template;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.proj.core.ProjEnumChoiceFetchHandler;
import xyz.herz.ep.proj.enums.ProjDictEnums.EnableStatus;
import xyz.herz.ep.proj.handler.master.ProjTemplateToggleHandler;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目模板(参考 ERPNext Project Template)。
 * <p>预置任务清单,新建项目时可「按模板克隆」一键生成项目 + 任务。
 * status: ENABLED(1)/DISABLED(0),通过行按钮启停切换。
 */
@Getter @Setter
@Entity
@Table(name = "proj_project_template")
@Erupt(
    name = "项目模板",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "启用", code = ProjTemplateToggleHandler.CODE_ENABLE,
            operationHandler = ProjTemplateToggleHandler.class,
            operationParam = { ProjTemplateToggleHandler.CODE_ENABLE }),
        @RowOperation(title = "停用", code = ProjTemplateToggleHandler.CODE_DISABLE,
            operationHandler = ProjTemplateToggleHandler.class,
            operationParam = { ProjTemplateToggleHandler.CODE_DISABLE })
    }
)
public class ProjProjectTemplate extends MetaModelVo {

    @EruptField(views = @View(title = "模板编码"),
                edit = @Edit(title = "模板编码", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "模板名称"),
                edit = @Edit(title = "模板名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "模板任务行", type = EditType.TAB_TABLE_ADD))
    private List<ProjProjectTemplateTask> tasks = new ArrayList<>();
}
