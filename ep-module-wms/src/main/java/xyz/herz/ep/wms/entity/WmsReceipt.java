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
import xyz.herz.ep.wms.enums.WmsDictEnums.ReceiptStatus;
import xyz.herz.ep.wms.handler.WmsReceiptCompleteHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 收货单。
 * <p>状态机:NEW(0) → RECEIVING(10 收货中) → COMPLETED(20 已完成)。
 * 完成时由 {@link xyz.herz.ep.wms.handler.WmsReceiptCompleteHandler} 回写 ASN receivedQty + 推进 ASN 状态。
 */
@Getter @Setter
@Entity
@Table(name = "wms_receipt")
@Erupt(
    name = "收货单",
    power = @Power(importable = true, export = true),
    dataProxy = WmsReceipt.Proxy.class,
    rowOperation = {
        @RowOperation(title = "完成收货", code = WmsReceiptCompleteHandler.CODE_COMPLETE, icon = "fa fa-check-circle",
            operationHandler = WmsReceiptCompleteHandler.class,
            operationParam = { WmsReceiptCompleteHandler.CODE_COMPLETE })
    }
)
public class WmsReceipt extends BaseModel {

    @EruptField(views = @View(title = "收货单号"),
                edit = @Edit(title = "收货单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "ASN ID"),
                edit = @Edit(title = "ASN ID", desc = "关联的入库通知单 ID"))
    @Column(name = "asn_id")
    private Long asnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsWarehouse warehouse;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ReceiptStatus")))
    private Integer status = ReceiptStatus.NEW.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "receipt_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "收货明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsReceiptItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsReceipt> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ ReceiptStatus.NEW.code, null };
        }
    }
}
