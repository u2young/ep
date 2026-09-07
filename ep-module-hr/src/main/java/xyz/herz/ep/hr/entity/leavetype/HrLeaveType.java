package xyz.herz.ep.hr.entity.leavetype;

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
import java.math.BigDecimal;

/**
 * 请假类型(参考 ERPNext Leave Type DocType,主数据)。
 * <p>定义每种假期的最大天数与是否带薪,请假单引用此实体。
 */
@Getter @Setter
@Entity
@Table(name = "hr_leave_type")
@Erupt(name = "请假类型", power = @Power(importable = true, export = true))
public class HrLeaveType extends MetaModelVo {

    @EruptField(views = @View(title = "编码"),
                edit = @Edit(title = "编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "名称"),
                edit = @Edit(title = "名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "最大天数"),
                edit = @Edit(title = "最大天数", desc = "单次/年度上限,0 表示不限"))
    @Column(name = "max_days", precision = 6, scale = 1)
    private BigDecimal maxDays;

    @EruptField(views = @View(title = "是否带薪"),
                edit = @Edit(title = "是否带薪"))
    @Column(name = "is_paid")
    private Boolean isPaid = true;

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
