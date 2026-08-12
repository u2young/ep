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
import xyz.herz.ep.wms.enums.WmsDictEnums.PutawayStatus;
import xyz.herz.ep.wms.handler.WmsPutawayCompleteHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 上架单。
 * <p>状态机:NEW(0) → PUTTING(10 上架中) → COMPLETED(20 已完成)。
 * 完成时由 {@link xyz.herz.ep.wms.handler.WmsPutawayCompleteHandler} 更新 WmsStock availableQty + 写流水。
 */
@Getter @Setter
@Entity
@Table(name = "wms_putaway")
@Erupt(
    name = "上架单",
    power = @Power(importable = true, export = true),
    dataProxy = WmsPutaway.Proxy.class,
    rowOperation = {
        @RowOperation(title = "完成上架", code = WmsPutawayCompleteHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = WmsPutawayCompleteHandler.class,
            operationParam = { WmsPutawayCompleteHandler.CODE_COMPLETE })
    }
)
public class WmsPutaway extends BaseModel {

    @EruptField(views = @View(title = "上架单号"),
                edit = @Edit(title = "上架单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "收货单 ID"),
                edit = @Edit(title = "收货单 ID"))
    @Column(name = "receipt_id")
    private Long receiptId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PutawayStatus")))
    private Integer status = PutawayStatus.NEW.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "putaway_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "上架明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsPutawayItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsPutaway> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ PutawayStatus.NEW.code, null };
        }
    }
}
