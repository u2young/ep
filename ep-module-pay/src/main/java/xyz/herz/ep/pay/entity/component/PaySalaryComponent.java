package xyz.herz.ep.pay.entity.component;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pay.core.PayEnumChoiceFetchHandler;
import xyz.herz.ep.pay.enums.PayDictEnums.ComponentType;
import xyz.herz.ep.pay.enums.PayDictEnums.EnableStatus;

import jakarta.persistence.*;

/**
 * 工资项组件(参考 ERPNext Salary Component DocType)。
 * <p>收入/扣款的基本单元:基本工资/绩效奖金/社保/公积金/个税等。
 */
@Getter @Setter
@Entity
@Table(name = "pay_salary_component")
@Erupt(name = "工资项组件", power = @Power(importable = true, export = true))
public class PaySalaryComponent extends MetaModelVo {

    @EruptField(views = @View(title = "组件编码"),
                edit = @Edit(title = "组件编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "组件名称"),
                edit = @Edit(title = "组件名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "类型"),
                edit = @Edit(title = "类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PayEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ComponentType")))
    @Column(name = "component_type", nullable = false)
    private Integer componentType = ComponentType.EARNING.code;

    @EruptField(views = @View(title = "是否计税"),
                edit = @Edit(title = "是否计税", type = EditType.BOOLEAN,
                    boolType = @BoolType(trueText = "计税", falseText = "免税")))
    @Column(name = "is_taxable")
    private Boolean isTaxable = true;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PayEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
