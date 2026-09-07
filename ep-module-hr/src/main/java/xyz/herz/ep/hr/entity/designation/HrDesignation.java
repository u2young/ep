package xyz.herz.ep.hr.entity.designation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.hr.core.HrEnumChoiceFetchHandler;
import xyz.herz.ep.hr.enums.HrDictEnums.EnableStatus;

import jakarta.persistence.*;

/**
 * 职位/岗位(参考 ERPNext Designation DocType,主数据)。
 * <p>员工主档引用此实体作为岗位。
 */
@Getter @Setter
@Entity
@Table(name = "hr_designation")
@Erupt(name = "职位", power = @Power(importable = true, export = true))
public class HrDesignation extends MetaModelVo {

    @EruptField(views = @View(title = "职位编码"),
                edit = @Edit(title = "职位编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "职位名称"),
                edit = @Edit(title = "职位名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "职位描述"),
                edit = @Edit(title = "职位描述"))
    @Column(length = 500)
    private String description;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
