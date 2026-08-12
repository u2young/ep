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
import xyz.herz.ep.mp.core.MpStateDataProxy;
import xyz.herz.ep.mp.enums.MpDictEnums.MenuStatus;
import xyz.herz.ep.mp.handler.MpMenuPublishHandler;
import xyz.herz.ep.mp.handler.MpMenuRevokeHandler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 公众号菜单。树形(parentId 自关联)。
 * 状态机:status 0=草稿 / 1=已发布。
 * <p>禁止表单直接改 status,只能通过【发布】(0→1) /【撤回】(1→0)行按钮变更。
 */
@Getter @Setter
@Entity
@Table(name = "mp_menu")
@Erupt(
    name = "公众号菜单",
    power = @Power(importable = true, export = true),
    dataProxy = MpMenu.Proxy.class,
    rowOperation = {
        @RowOperation(title = "发布", code = "PUBLISH", icon = "fa fa-cloud-upload",
            operationHandler = MpMenuPublishHandler.class),
        @RowOperation(title = "撤回", code = "REVOKE", icon = "fa fa-undo",
            operationHandler = MpMenuRevokeHandler.class)
    }
)
public class MpMenu extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "父菜单ID"),
                edit = @Edit(title = "父菜单ID", desc = "0=一级菜单"))
    private Long parentId = 0L;

    @EruptField(views = @View(title = "菜单名称"),
                edit = @Edit(title = "菜单名称", notNull = true, search = @Search))
    @Column(length = 255, nullable = false)
    private String name;

    @EruptField(views = @View(title = "按钮类型"),
                edit = @Edit(title = "按钮类型", desc = "click=点击推事件 / view=跳转网页"))
    @Column(length = 32)
    private String type;

    @EruptField(views = @View(title = "菜单KEY"),
                edit = @Edit(title = "菜单KEY", desc = "click 类型必填"))
    @Column(length = 255)
    private String menuKey;

    @EruptField(views = @View(title = "网页URL"),
                edit = @Edit(title = "网页URL", desc = "view 类型必填"))
    @Column(length = 500)
    private String url;

    @EruptField(views = @View(title = "排序", sortable = true),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "状态", sortable = true),
                edit = @Edit(title = "状态", notNull = true,
                    desc = "禁止直接修改,请通过【发布/撤回】按钮变更",
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MenuStatus")))
    private Integer status = MenuStatus.DRAFT.code;

    public static class Proxy extends MpStateDataProxy<MpMenu> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ MenuStatus.DRAFT.code, null };
        }
    }
}
