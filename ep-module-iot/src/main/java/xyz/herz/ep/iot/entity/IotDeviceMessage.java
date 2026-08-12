package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 设备消息记录(上行/下行)。payload 存原始报文。 */
@Getter 
@Setter
@Entity
@Table(name = "iot_device_message")
@Erupt(name = "设备消息", power = @Power(export = true, add = false))
public class IotDeviceMessage extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    @EruptField(views = @View(title = "设备", column = "code"),
                edit = @Edit(title = "设备", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private IotDevice device;

    @EruptField(views = @View(title = "消息ID"),
                edit = @Edit(title = "消息ID"))
    @Column(length = 64)
    private String messageId;

    @EruptField(views = @View(title = "方向"),
                edit = @Edit(title = "方向", notNull = true,
                    choiceType = @ChoiceType(vl = {
                        @VL(value = "1", label = "上行"),
                        @VL(value = "2", label = "下行")
                    })))
    @Column(nullable = false)
    private Integer direction;

    @EruptField(views = @View(title = "消息类型"),
                edit = @Edit(title = "消息类型"))
    @Column(length = 32)
    private String type;

    @EruptField(views = @View(title = "报文内容"),
                edit = @Edit(title = "报文内容", type = EditType.TEXTAREA))
    @Lob
    @Column(columnDefinition = "text")
    private String payload;

    @EruptField(views = @View(title = "消息时间", sortable = true),
                edit = @Edit(title = "消息时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
