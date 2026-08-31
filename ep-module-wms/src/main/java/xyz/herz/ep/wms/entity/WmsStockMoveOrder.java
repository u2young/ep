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

import xyz.erupt.annotation.sub_field.sub_edit.ButtonType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.core.WmsStateDataProxy;
import xyz.herz.ep.wms.enums.WmsDictEnums.StockMoveOrderStatus;
import xyz.herz.ep.wms.handler.WmsMoveRecommendButtonHandler;
import xyz.herz.ep.wms.handler.WmsStockMoveCompleteHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 移库作业单。
 * <p>状态机:NEW(0) → MOVING(10 移库中) → COMPLETED(20 已完成)。
 * 完成时由 {@link xyz.herz.ep.wms.handler.WmsStockMoveCompleteHandler} 对每个明细写一条 MOVE 流水。
 */
@Getter @Setter
@Entity
@Table(name = "wms_stock_move_order")
@Erupt(
    name = "移库作业",
    power = @Power(importable = true, export = true),
    dataProxy = WmsStockMoveOrder.Proxy.class,
    rowOperation = {
        @RowOperation(title = "完成", code = WmsStockMoveCompleteHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = WmsStockMoveCompleteHandler.class,
            operationParam = { WmsStockMoveCompleteHandler.CODE_COMPLETE })
    }
)
public class WmsStockMoveOrder extends BaseModel {

    @EruptField(views = @View(title = "移库单号"),
                edit = @Edit(title = "移库单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsWarehouse warehouse;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockMoveOrderStatus")))
    private Integer status = StockMoveOrderStatus.NEW.code;

    @Transient
    @EruptField(
        views = @View(title = "推荐移库数触发器", show = false),
        edit = @Edit(
            title = "推荐移库数（按源库位可用库存自动 clamp）",
            type = EditType.BUTTON,
            desc = "点按钮自动根据每条明细源库位实际可用库存,把明细 qty clamp 到最大可移数量",
            buttonType = @ButtonType(
                handler = WmsMoveRecommendButtonHandler.class,
                icon = "fa fa-balance-scale",
                confirm = "根据源库位实际可用库存自动调整全单移库数量(会直接覆盖明细 qty),确认继续?",
                style = "primary"
            )
        )
    )
    private Integer runRecommendQtyTrigger = 1;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "move_order_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "移库明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsStockMoveOrderItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsStockMoveOrder> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ StockMoveOrderStatus.NEW.code, null };
        }
    }
}
