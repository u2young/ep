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
import xyz.herz.ep.wms.enums.WmsDictEnums.StockCheckStatus;
import xyz.herz.ep.wms.handler.WmsStockCheckFinishHandler;
import xyz.herz.ep.wms.handler.WmsStockCheckStartHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 盘点单。
 * <p>状态机:NEW(0) → CHECKING(10 盘点中) → COMPLETED(20 已完成) / HAS_DIFF(30 有差异)。
 * 开始盘点时由 {@link WmsStockCheckStartHandler} 回写 bookQty,
 * 完成盘点时由 {@link WmsStockCheckFinishHandler} 计算 diffQty 并推进状态。
 */
@Getter @Setter
@Entity
@Table(name = "wms_stock_check")
@Erupt(
    name = "盘点单",
    power = @Power(importable = true, export = true),
    dataProxy = WmsStockCheck.Proxy.class,
    rowOperation = {
        @RowOperation(title = "开始盘点", code = WmsStockCheckStartHandler.CODE_START, icon = "fa fa-play",
            operationHandler = WmsStockCheckStartHandler.class,
            operationParam = { WmsStockCheckStartHandler.CODE_START }),
        @RowOperation(title = "完成盘点", code = WmsStockCheckFinishHandler.CODE_FINISH, icon = "fa fa-check-circle",
            operationHandler = WmsStockCheckFinishHandler.class,
            operationParam = { WmsStockCheckFinishHandler.CODE_FINISH })
    }
)
public class WmsStockCheck extends BaseModel {

    @EruptField(views = @View(title = "盘点单号"),
                edit = @Edit(title = "盘点单号", notNull = true))
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
                        fetchHandlerParams = "StockCheckStatus")))
    private Integer status = StockCheckStatus.NEW.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "check_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "盘点明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsStockCheckItem> items = new ArrayList<>();

    /** 虚拟(非持久化): 盘点进度百分比 = sum(actualQty) / sum(bookQty) * 100,PROGRESS 视图。 */
    @Transient
    @EruptField(views = @View(title = "盘点进度", type = xyz.erupt.annotation.sub_field.ViewType.PROGRESS))
    private BigDecimal checkProgress;

    public BigDecimal getCheckProgress() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        int totalBook = 0, totalActual = 0;
        for (WmsStockCheckItem it : items) {
            totalBook += it.getBookQty() != null ? it.getBookQty() : 0;
            totalActual += it.getActualQty() != null ? it.getActualQty() : 0;
        }
        if (totalBook == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalActual).multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(totalBook), 2, RoundingMode.HALF_UP);
    }

    public static class Proxy extends WmsStateDataProxy<WmsStockCheck> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ StockCheckStatus.NEW.code, null };
        }
    }
}
