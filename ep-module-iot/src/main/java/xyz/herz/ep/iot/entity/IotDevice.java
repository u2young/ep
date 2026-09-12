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
import xyz.herz.ep.iot.enums.IotDictEnums.DeviceStatus;
import xyz.herz.ep.iot.handler.IotDeviceActivateHandler;
import xyz.herz.ep.iot.handler.IotDeviceDisableHandler;
import xyz.herz.ep.iot.handler.IotDeviceEnableHandler;
import xyz.herz.ep.iot.handler.IotDeviceSendCommandHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设备。status 为生命周期状态机(未激活/在线/离线/禁用),只能通过行按钮变更:
 * 激活(未激活→离线)、启用(禁用→离线)、禁用(在线/离线→禁用)。
 */
@Getter @Setter
@Entity
@Table(name = "iot_device")
@Erupt(
    name = "设备管理",
    dataProxy = IotDevice.Proxy.class,
    power = @Power(importable = true, export = true),
    layout = @Layout(collapseActionButton = true),
    rowOperation = {
        @RowOperation(title = "激活", code = "ACTIVATE", icon = "fa fa-bolt",
            operationHandler = IotDeviceActivateHandler.class, operationParam = { "ACTIVATE" }),
        @RowOperation(title = "启用", code = "ENABLE", icon = "fa fa-play",
            operationHandler = IotDeviceEnableHandler.class, operationParam = { "ENABLE" }),
        @RowOperation(title = "禁用", code = "DISABLE", icon = "fa fa-stop",
            operationHandler = IotDeviceDisableHandler.class, operationParam = { "DISABLE" })
    }
)
public class IotDevice extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private IotProduct product;

    @EruptField(views = @View(title = "设备名称"),
                edit = @Edit(title = "设备名称", notNull = true))
    @Column(length = 128, nullable = false)
    private String name;

    @EruptField(views = @View(title = "设备编码/序列号"),
                edit = @Edit(title = "设备编码/序列号", notNull = true))
    @Column(length = 64, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "设备昵称"),
                edit = @Edit(title = "设备昵称"))
    @Column(length = 128)
    private String nickname;

    @EruptField(views = @View(title = "设备状态"),
                edit = @Edit(title = "设备状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = IotEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "DeviceStatus")))
    private Integer status = DeviceStatus.INACTIVE.code;

    @EruptField(
        views = @View(title = "设备密钥", type = xyz.erupt.annotation.sub_field.ViewType.PASSWORD),
        edit = @Edit(title = "设备密钥", type = EditType.PASSWORD, show = false)
    )
    @Column(length = 128)
    private String deviceSecret;

    @EruptField(views = @View(title = "最后上线时间"),
                edit = @Edit(title = "最后上线时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "last_online_time")
    private LocalDateTime lastOnlineTime;

    /** 预留:关联固件 ID(OTA 模块未实现时仅存 id)。 */
    @Column(name = "firmware_id")
    private Long firmwareId;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    /** 锁定 status 字段:只能通过行按钮改,不能表单直接写。 */
    public static class Proxy extends IotStateDataProxy<IotDevice> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ null };
        }
    }
}
