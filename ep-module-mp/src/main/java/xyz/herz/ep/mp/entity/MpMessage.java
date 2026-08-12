package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 消息记录。统一收发消息一张表,direction 区分方向。 */
@Getter @Setter
@Entity
@Table(name = "mp_message")
@Erupt(name = "消息记录", power = @Power(importable = true, export = true))
public class MpMessage extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "发送方"), edit = @Edit(title = "发送方"))
    @Column(length = 64)
    private String fromUser;

    @EruptField(views = @View(title = "接收方"), edit = @Edit(title = "接收方"))
    @Column(length = 64)
    private String toUser;

    @EruptField(views = @View(title = "消息类型", sortable = true),
                edit = @Edit(title = "消息类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MessageType")))
    @Column(length = 32, nullable = false)
    private String msgType;

    @Lob
    @EruptField(views = @View(title = "消息内容"),
                edit = @Edit(title = "消息内容", type = EditType.TEXTAREA))
    private String content;

    @EruptField(views = @View(title = "方向", sortable = true),
                edit = @Edit(title = "方向", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MessageDirection")))
    private Integer direction;

    @EruptField(views = @View(title = "消息时间", sortable = true),
                edit = @Edit(title = "消息时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime createTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
