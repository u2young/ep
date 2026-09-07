package xyz.herz.ep.pay.entity.structure;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.pay.entity.component.PaySalaryComponent;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 工资结构明细(参考 ERPNext Salary Structure 的 earnings/deductions 子表)。
 * <p>组件 + 固定金额;工资单提交时按此快照生成 PaySalarySlipItem。
 */
@Getter @Setter
@Entity
@Table(name = "pay_salary_structure_item")
public class PaySalaryStructureItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private PaySalaryStructure structure;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "component_id", nullable = true)
    @EruptField(views = @View(title = "组件", column = "name"),
                edit = @Edit(title = "组件", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private PaySalaryComponent component;

    @EruptField(views = @View(title = "金额"),
                edit = @Edit(title = "金额", notNull = true))
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;
}
