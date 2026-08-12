package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.core.WmsStateDataProxy;
import xyz.herz.ep.wms.enums.WmsDictEnums.ShipmentNoticeStatus;
import xyz.herz.ep.wms.handler.WmsShipmentNoticeCloseHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 出库通知单。
 * <p>状态机:NEW(0) → PARTIAL_PICKED(10 部分拣货) → PICKED(20 已拣货) → CLOSED(30 关闭)。
 * 拣货单完成时由 {@link xyz.herz.ep.wms.handler.WmsPickCompleteHandler} 回写 pickedQty + 推进通知状态。
 */
@Getter @Setter
@Entity
@Table(name = "wms_shipment_notice")
@Erupt(
    name = "出库通知单",
    power = @Power(importable = true, export = true),
    dataProxy = WmsShipmentNotice.Proxy.class,
    rowOperation = {
        @RowOperation(title = "关闭", code = WmsShipmentNoticeCloseHandler.CODE_CLOSE, icon = "fa fa-lock",
            operationHandler = WmsShipmentNoticeCloseHandler.class,
            operationParam = { WmsShipmentNoticeCloseHandler.CODE_CLOSE })
    }
)
public class WmsShipmentNotice extends BaseModel {

    @EruptField(views = @View(title = "通知单号"),
                edit = @Edit(title = "通知单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsWarehouse warehouse;

    @EruptField(views = @View(title = "客户"),
                edit = @Edit(title = "客户"))
    @Column(length = 200)
    private String customerName;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ShipmentNoticeStatus")))
    private Integer status = ShipmentNoticeStatus.NEW.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "notice_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "出库明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsShipmentItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsShipmentNotice> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ ShipmentNoticeStatus.NEW.code, null };
        }
    }
}
