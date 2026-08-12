package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.core.WmsStateDataProxy;
import xyz.herz.ep.wms.enums.WmsDictEnums.PickStatus;
import xyz.herz.ep.wms.handler.WmsPickCompleteHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 拣货单。
 * <p>状态机:NEW(0) → PICKING(10 拣货中) → COMPLETED(20 已完成)。
 * 完成时由 {@link xyz.herz.ep.wms.handler.WmsPickCompleteHandler} 扣减 WmsStock(available/locked) + 回写 Notice pickedQty。
 */
@Getter @Setter
@Entity
@Table(name = "wms_pick")
@Erupt(
    name = "拣货单",
    power = @Power(importable = true, export = true),
    dataProxy = WmsPick.Proxy.class,
    rowOperation = {
        @RowOperation(title = "完成拣货", code = WmsPickCompleteHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = WmsPickCompleteHandler.class,
            operationParam = { WmsPickCompleteHandler.CODE_COMPLETE })
    }
)
public class WmsPick extends BaseModel {

    @EruptField(views = @View(title = "拣货单号"),
                edit = @Edit(title = "拣货单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "出库通知 ID"),
                edit = @Edit(title = "出库通知 ID"))
    @Column(name = "notice_id")
    private Long noticeId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PickStatus")))
    private Integer status = PickStatus.NEW.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pick_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "拣货明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsPickItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsPick> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ PickStatus.NEW.code, null };
        }
    }
}
