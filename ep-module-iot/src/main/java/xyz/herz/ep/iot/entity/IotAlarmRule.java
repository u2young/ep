package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ButtonType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.iot.core.IotEnumChoiceFetchHandler;
import xyz.herz.ep.iot.enums.IotDictEnums.EnableStatus;
import xyz.herz.ep.iot.handler.IotAlarmRuleValidateButtonHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 告警规则。device 可空(空=产品级规则)。condition 存 JSON 判断条件。 */
@Getter @Setter
@Entity
@Table(name = "iot_alarm_rule")
@Erupt(name = "告警规则", power = @Power(importable = true, export = true))
public class IotAlarmRule extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private IotProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @EruptField(views = @View(title = "设备", column = "code"),
                edit = @Edit(title = "设备", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private IotDevice device;

    @EruptField(views = @View(title = "物模型标识符"),
                edit = @Edit(title = "物模型标识符"))
    @Column(length = 64)
    private String identifier;

    @EruptField(views = @View(title = "规则名称"),
                edit = @Edit(title = "规则名称", notNull = true))
    @Column(length = 128, nullable = false)
    private String name;

    @EruptField(views = @View(title = "规则类型"),
                edit = @Edit(title = "规则类型", notNull = true,
                    choiceType = @ChoiceType(vl = {
                        @VL(value = "threshold", label = "阈值"),
                        @VL(value = "state", label = "状态")
                    })))
    @Column(length = 32, nullable = false)
    private String ruleType;

    @EruptField(views = @View(title = "判断条件"),
                edit = @Edit(title = "判断条件(JSON)", type = EditType.TEXTAREA))
    @Lob
    @Column(columnDefinition = "text")
    private String condition;

    @EruptField(views = @View(title = "阈值"),
                edit = @Edit(title = "阈值", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal threshold;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    // =================== BUTTON 辅助输入字段(均 @Transient 不持久化) ===================

    /** 输入一条样例数值(如温湿度上报值),点按钮验证规则是否命中。 */
    @Transient
    @EruptField(
        views = @View(title = "触发验证-样例值", show = false),
        edit = @Edit(
            title = "样例数值(验证规则)",
            type = EditType.BUTTON,
            desc = "填入一条模拟上报的数值,点右侧按钮验证当前规则是否会触发告警。阈值型规则:X ≥ T+5 → 命中",
            numberType = @NumberType,
            buttonType = @ButtonType(
                handler = IotAlarmRuleValidateButtonHandler.class,
                icon = "fa fa-flask",
                confirm = "用当前填写的样例数值执行一次命中验证,确认继续?",
                style = "warning"
            )
        )
    )
    private BigDecimal validateSample;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;
}
