package xyz.herz.ep.qal.entity.criteria;

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
import xyz.herz.ep.qal.core.QalEnumChoiceFetchHandler;
import xyz.herz.ep.qal.enums.QalDictEnums.EnableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 质检参数标准(参考 ERPNext Quality Inspection Parameter + 模板行)。
 * <p>定义可检验参数的规格上下限:读数值在 [lowerLimit, upperLimit] 内视为合格;
 * limit 为空表示该参数不做数值判定(目测类,由检查员给结论)。
 * <p>质检单提交时,按启用的参数快照生成读数明细。
 */
@Getter @Setter
@Entity
@Table(name = "qal_criteria")
@Erupt(name = "质检参数标准", power = @Power(importable = true, export = true))
public class QalCriteria extends MetaModelVo {

    @EruptField(views = @View(title = "参数编码"),
                edit = @Edit(title = "参数编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "参数名称"),
                edit = @Edit(title = "参数名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "计量单位"),
                edit = @Edit(title = "计量单位", desc = "如 mm/g/kN,目测类可为空"))
    @Column(length = 20)
    private String unit;

    @EruptField(views = @View(title = "规格下限"),
                edit = @Edit(title = "规格下限", desc = "为空表示不设下限"))
    @Column(name = "lower_limit", precision = 18, scale = 4)
    private java.math.BigDecimal lowerLimit;

    @EruptField(views = @View(title = "规格上限"),
                edit = @Edit(title = "规格上限", desc = "为空表示不设上限"))
    @Column(name = "upper_limit", precision = 18, scale = 4)
    private java.math.BigDecimal upperLimit;

    @EruptField(views = @View(title = "是否必检"),
                edit = @Edit(title = "是否必检", desc = "必检参数在质检单提交时必须录入读数"))
    @Column(name = "is_must")
    private Boolean isMust = true;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = QalEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
