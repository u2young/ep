package xyz.herz.ep.pay.entity.structure;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pay.core.PayEnumChoiceFetchHandler;
import xyz.herz.ep.pay.enums.PayDictEnums.EnableStatus;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 工资结构(参考 ERPNext Salary Structure DocType)。
 * <p>定义员工工资构成模板:收入项 + 扣款项。工资单提交时从此结构快照生成明细。
 */
@Getter @Setter
@Entity
@Table(name = "pay_salary_structure")
@Erupt(name = "工资结构", power = @Power(importable = true, export = true))
public class PaySalaryStructure extends MetaModelVo {

    @EruptField(views = @View(title = "结构编码"),
                edit = @Edit(title = "结构编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "结构名称"),
                edit = @Edit(title = "结构名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @OneToMany(mappedBy = "structure", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "结构明细", type = EditType.TAB_TABLE_ADD))
    private List<PaySalaryStructureItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "是否启用"),
                edit = @Edit(title = "是否启用"))
    @Column(name = "is_active")
    private Boolean isActive = true;

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
