package xyz.herz.ep.erp.entity.master;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.MetaModelVo;

import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 计量单位(个/箱/千克/米等)。 */
@Getter @Setter
@Entity
@Table(name = "erp_product_unit")
@Erupt(name = "产品单位")
public class ErpProductUnit extends MetaModelVo {

    @EruptField(
        views = @View(title = "单位名称"),
        edit = @Edit(title = "单位名称", notNull = true,
            inputType = @InputType)
    )
    @Column(length = 50, nullable = false, unique = true)
    private String name;

    @EruptField(
        views = @View(title = "英文符号"),
        edit = @Edit(title = "英文符号", inputType = @InputType)
    )
    @Column(length = 20)
    private String symbol;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
