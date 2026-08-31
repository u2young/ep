package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTreeType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.iot.core.IotEnumChoiceFetchHandler;
import xyz.herz.ep.iot.enums.IotDictEnums.EnableStatus;
import xyz.herz.ep.iot.enums.IotDictEnums.NetType;
import xyz.herz.ep.iot.enums.IotDictEnums.NodeType;
import xyz.herz.ep.iot.handler.IotProductToggleHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 产品档案。status(启停) 通过行按钮切换。 */
@Getter @Setter
@Entity
@Table(name = "iot_product")
@Erupt(
    name = "产品档案",
    power = @Power(importable = true, export = true, copy = true),
    rowOperation = {
        @RowOperation(title = "启用", code = IotProductToggleHandler.ENABLE, icon = "fa fa-check",
            operationHandler = IotProductToggleHandler.class, operationParam = { IotProductToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = IotProductToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = IotProductToggleHandler.class, operationParam = { IotProductToggleHandler.DISABLE })
    }
)
public class IotProduct extends BaseModel {

    @EruptField(views = @View(title = "产品名称"),
                edit = @Edit(title = "产品名称", notNull = true))
    @Column(length = 255, nullable = false)
    private String name;

    @EruptField(views = @View(title = "产品编码"),
                edit = @Edit(title = "产品编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @EruptField(views = @View(title = "分类", column = "name"),
                edit = @Edit(title = "分类", type = EditType.REFERENCE_TREE,
                    referenceTreeType = @ReferenceTreeType(id = "id", label = "name")))
    private IotProductCategory category;

    @EruptField(views = @View(title = "节点类型"),
                edit = @Edit(title = "节点类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "NodeType")))
    private Integer nodeType = NodeType.DIRECT.code;

    @EruptField(views = @View(title = "联网方式"),
                edit = @Edit(title = "联网方式", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "NetType")))
    @Column(length = 32, nullable = false)
    private String netType = NetType.WIFI.code;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "描述"),
                edit = @Edit(title = "描述"))
    @Column(length = 500)
    private String description;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;
}
