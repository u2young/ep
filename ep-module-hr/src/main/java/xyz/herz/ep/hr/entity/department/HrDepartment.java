package xyz.herz.ep.hr.entity.department;

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
 * 部门(参考 ERPNext Department DocType,树形主数据)。
 * <p>parentId 实现树形结构;isGroup 标记是否汇总节点(非叶子)。
 */
@Getter @Setter
@Entity
@Table(name = "hr_department")
@Erupt(name = "部门", power = @Power(importable = true, export = true))
public class HrDepartment extends MetaModelVo {

    @EruptField(views = @View(title = "部门编码"),
                edit = @Edit(title = "部门编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "部门名称"),
                edit = @Edit(title = "部门名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "上级部门ID"),
                edit = @Edit(title = "上级部门ID", desc = "顶层部门留空"))
    @Column(name = "parent_id")
    private Long parentId;

    @EruptField(views = @View(title = "是否组"),
                edit = @Edit(title = "是否组", desc = "组节点不可挂员工,仅做汇总"))
    @Column(name = "is_group")
    private Boolean isGroup = false;

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
