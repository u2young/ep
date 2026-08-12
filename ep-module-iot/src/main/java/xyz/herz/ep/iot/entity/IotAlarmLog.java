package xyz.herz.ep.iot.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 告警状态变更日志。每次 PROCESS/RESOLVE/IGNORE 写一条。 */
@Getter @Setter
@Entity
@Table(name = "iot_alarm_log")
@Erupt(name = "告警日志", power = @Power(export = true, add = false, edit = false))
public class IotAlarmLog extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alarm_id", nullable = false)
    @EruptField(views = @View(title = "告警", column = "id"),
                edit = @Edit(title = "告警", notNull = true))
    private IotAlarm alarm;

    @EruptField(views = @View(title = "变更前状态"),
                edit = @Edit(title = "变更前状态"))
    @Column(name = "from_status")
    private Integer fromStatus;

    @EruptField(views = @View(title = "变更后状态"),
                edit = @Edit(title = "变更后状态"))
    @Column(name = "to_status")
    private Integer toStatus;

    @EruptField(views = @View(title = "操作人ID"),
                edit = @Edit(title = "操作人ID"))
    @Column(name = "operator_id")
    private Long operatorId;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 1000)
    private String remark;

    @EruptField(views = @View(title = "操作时间", sortable = true),
                edit = @Edit(title = "操作时间", type = xyz.erupt.annotation.sub_field.EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
