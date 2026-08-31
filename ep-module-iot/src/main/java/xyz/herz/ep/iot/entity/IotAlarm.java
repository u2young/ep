package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.iot.core.IotEnumChoiceFetchHandler;
import xyz.herz.ep.iot.core.IotStateDataProxy;
import xyz.herz.ep.iot.enums.IotDictEnums.AlarmLevel;
import xyz.herz.ep.iot.enums.IotDictEnums.AlarmStatus;
import xyz.herz.ep.iot.handler.IotAlarmIgnoreHandler;
import xyz.herz.ep.iot.handler.IotAlarmProcessHandler;
import xyz.herz.ep.iot.handler.IotAlarmResolveHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 告警。status 为状态机(待处理→处理中→已解决/已忽略),只能通过行按钮变更:
 * 处理(0→10)、解决(10→20,设置 resolveTime)、忽略(0/10→30)。
 */
@Getter @Setter
@Entity
@Table(name = "iot_alarm")
@Erupt(
    name = "告警中心",
    dataProxy = IotAlarm.Proxy.class,
    power = @Power(importable = true, export = true),
    layout = @Layout(collapseActionButton = true),
    rowOperation = {
        @RowOperation(title = "处理", code = "PROCESS", icon = "fa fa-cogs",
            operationHandler = IotAlarmProcessHandler.class, operationParam = { "PROCESS" }),
        @RowOperation(title = "解决", code = "RESOLVE", icon = "fa fa-check",
            operationHandler = IotAlarmResolveHandler.class, operationParam = { "RESOLVE" }),
        @RowOperation(title = "忽略", code = "IGNORE", icon = "fa fa-times",
            operationHandler = IotAlarmIgnoreHandler.class, operationParam = { "IGNORE" })
    }
)
public class IotAlarm extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    @EruptField(views = @View(title = "规则", column = "name"),
                edit = @Edit(title = "规则", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private IotAlarmRule rule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @EruptField(views = @View(title = "设备", column = "code"),
                edit = @Edit(title = "设备", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private IotDevice device;

    @EruptField(views = @View(title = "级别"),
                edit = @Edit(title = "级别", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AlarmLevel")))
    private Integer level = AlarmLevel.WARNING.code;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AlarmStatus")))
    private Integer status = AlarmStatus.PENDING.code;

    @EruptField(views = @View(title = "触发时间", sortable = true),
                edit = @Edit(title = "触发时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "trigger_time")
    private LocalDateTime triggerTime;

    @EruptField(views = @View(title = "解决时间"),
                edit = @Edit(title = "解决时间", type = EditType.DATE, show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "resolve_time")
    private LocalDateTime resolveTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    /** 锁定 status 字段:只能通过行按钮改,不能表单直接写。 */
    public static class Proxy extends IotStateDataProxy<IotAlarm> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ null };
        }
    }
}
