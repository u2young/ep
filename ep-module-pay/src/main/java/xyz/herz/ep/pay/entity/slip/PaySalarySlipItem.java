package xyz.herz.ep.pay.entity.slip;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 工资单明细(提交时从工资结构快照生成,不 REF 组件实体,保持打印/过账独立)。
 */
@Getter @Setter
@Entity
@Table(name = "pay_salary_slip_item")
public class PaySalarySlipItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slip_id", nullable = false)
    private PaySalarySlip slip;

    @EruptField(views = @View(title = "组件编码"))
    @Column(name = "component_code", length = 50)
    private String componentCode;

    @EruptField(views = @View(title = "组件名称"),
                edit = @Edit(title = "组件名称", notNull = true))
    @Column(name = "component_name", length = 100, nullable = false)
    private String componentName;

    @EruptField(views = @View(title = "类型", desc = "1收入/2扣款"))
    @Column(name = "component_type", nullable = false)
    private Integer componentType;

    @EruptField(views = @View(title = "金额"),
                edit = @Edit(title = "金额", notNull = true))
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;
}
