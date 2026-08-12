package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler;
import xyz.herz.ep.mp.enums.MpDictEnums.EnableStatus;
import xyz.herz.ep.mp.handler.MpReplyToggleHandler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 自动回复。type 区分:1关注回复 / 2关键字回复 / 3默认回复。
 * status 启停通过行按钮(启用/禁用)切换。
 */
@Getter @Setter
@Entity
@Table(name = "mp_auto_reply")
@Erupt(
    name = "自动回复",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "启用", code = "ENABLE", icon = "fa fa-check",
            operationHandler = MpReplyToggleHandler.class, operationParam = { "ENABLE" }),
        @RowOperation(title = "禁用", code = "DISABLE", icon = "fa fa-ban",
            operationHandler = MpReplyToggleHandler.class, operationParam = { "DISABLE" })
    }
)
public class MpAutoReply extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "回复类型", sortable = true),
                edit = @Edit(title = "回复类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ReplyType")))
    private Integer type;

    @EruptField(views = @View(title = "匹配关键字"),
                edit = @Edit(title = "匹配关键字", desc = "type=2 关键字回复时必填"))
    @Column(length = 255)
    private String matchKeyword;

    @EruptField(views = @View(title = "回复内容类型"),
                edit = @Edit(title = "回复内容类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ReplyContentType")))
    @Column(length = 32, nullable = false)
    private String replyContentType;

    @EruptField(views = @View(title = "回复内容"),
                edit = @Edit(title = "回复内容", type = EditType.TEXTAREA))
    @Column(length = 1024)
    private String replyContent;

    @EruptField(views = @View(title = "状态", sortable = true),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.DISABLED.code;

    @EruptField(views = @View(title = "排序", sortable = true),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;
}
