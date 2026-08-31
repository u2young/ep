package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.enums.WmsDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 仓库档案。 */
@Getter @Setter
@Entity
@Table(name = "wms_warehouse")
@Erupt(name = "仓库档案",
    power = @Power(importable = true, export = true, copy = true))
public class WmsWarehouse extends BaseModel {

    @EruptField(views = @View(title = "仓库名称"),
                edit = @Edit(title = "仓库名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "仓库编码"),
                edit = @Edit(title = "仓库编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "详细地址"),
                edit = @Edit(title = "详细地址", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String address;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
