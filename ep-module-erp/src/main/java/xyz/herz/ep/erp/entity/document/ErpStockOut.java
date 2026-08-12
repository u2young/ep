package xyz.herz.ep.erp.entity.document;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.core.ErpStateDataProxy;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockDocStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockOutBizType;
import xyz.herz.ep.erp.handler.document.ErpStockDocCloseHandler;
import xyz.herz.ep.erp.handler.document.ErpStockOutAuditHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 其他出库单(盘亏/调拨出/其他)。
 * <p>状态机:DRAFT(0) → APPROVED(1,触发库存-) / UNAPPROVE(冲销) → CLOSED(2)。
 * 整单一仓,明细按 SKU 维度记录数量。
 */
@Getter @Setter
@Entity
@Table(name = "erp_stock_out")
@Erupt(
    name = "其他出库单",
    power = @Power(importable = true, export = true),
    dataProxy = ErpStockOut.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审核(出库)", code = ErpStockOutAuditHandler.CODE_AUDIT, icon = "fa fa-check-circle",
            operationHandler = ErpStockOutAuditHandler.class, operationParam = { ErpStockOutAuditHandler.CODE_AUDIT }),
        @RowOperation(title = "反审(冲销入库)", code = ErpStockOutAuditHandler.CODE_UNAUDIT, icon = "fa fa-undo",
            operationHandler = ErpStockOutAuditHandler.class, operationParam = { ErpStockOutAuditHandler.CODE_UNAUDIT }),
        @RowOperation(title = "关闭", code = ErpStockDocCloseHandler.CODE_CLOSE, icon = "fa fa-lock",
            operationHandler = ErpStockDocCloseHandler.class, operationParam = { ErpStockDocCloseHandler.CODE_CLOSE })
    }
)
public class ErpStockOut extends MetaModelVo {

    @EruptField(views = @View(title = "出库单号"),
                edit = @Edit(title = "出库单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "出库时间"),
                edit = @Edit(title = "出库时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(nullable = false)
    private LocalDateTime outTime = LocalDateTime.now();

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockDocStatus")))
    private Integer status = StockDocStatus.DRAFT.code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "出库仓库", column = "name"),
                edit = @Edit(title = "出库仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse warehouse;

    @EruptField(views = @View(title = "业务类型"),
                edit = @Edit(title = "业务类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockOutBizType")))
    @Column(nullable = false)
    private Integer bizType = StockOutBizType.OTHER.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "out_id")
    @EruptField(edit = @Edit(title = "出库明细", type = EditType.TAB_TABLE_ADD))
    private List<ErpStockOutItem> items = new ArrayList<>();

    public static class Proxy extends ErpStateDataProxy<ErpStockOut> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ StockDocStatus.DRAFT.code, null };
        }
    }
}
