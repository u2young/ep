package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.iot.core.IotEnumChoiceFetchHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 物模型(属性/服务/事件)。specs 存 JSON 数据规格。 */
@Getter @Setter
@Entity
@Table(name = "iot_thing_model")
@Erupt(name = "物模型", power = @Power(export = true))
public class IotThingModel extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private IotProduct product;

    @EruptField(views = @View(title = "标识符"),
                edit = @Edit(title = "标识符", notNull = true))
    @Column(length = 64, nullable = false)
    private String identifier;

    @EruptField(views = @View(title = "名称"),
                edit = @Edit(title = "名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "类型"),
                edit = @Edit(title = "类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ThingModelType")))
    private Integer type;

    @EruptField(views = @View(title = "数据类型"),
                edit = @Edit(title = "数据类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "DataType")))
    @Column(length = 32, nullable = false)
    private String dataType;

    @EruptField(views = @View(title = "访问模式"),
                edit = @Edit(title = "访问模式", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AccessMode")))
    private Integer accessMode;

    @EruptField(views = @View(title = "数据规格"),
                edit = @Edit(title = "数据规格(JSON)", type = EditType.TEXTAREA))
    @Lob
    @Column(columnDefinition = "text")
    private String specs;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;
}
