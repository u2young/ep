package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.core.WmsStateDataProxy;
import xyz.herz.ep.wms.enums.WmsDictEnums.AsnStatus;
import xyz.herz.ep.wms.handler.WmsAsnCloseHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ASN 入库通知单。
 * <p>状态机:NEW(0) → PARTIAL_RECEIVED(10 部分收货) → RECEIVED(20 已收货) → CLOSED(30 关闭)。
 * 收货单完成时由 {@link xyz.herz.ep.wms.handler.WmsReceiptCompleteHandler} 回写 receivedQty + 推进 ASN 状态。
 */
@Getter @Setter
@Entity
@Table(name = "wms_asn")
@Erupt(
    name = "入库通知单(ASN)",
    power = @Power(importable = true, export = true),
    dataProxy = WmsAsn.Proxy.class,
    rowOperation = {
        @RowOperation(title = "关闭", code = WmsAsnCloseHandler.CODE_CLOSE, icon = "fa fa-lock",
            operationHandler = WmsAsnCloseHandler.class,
            operationParam = { WmsAsnCloseHandler.CODE_CLOSE })
    }
)
public class WmsAsn extends BaseModel {

    @EruptField(views = @View(title = "ASN 单号"),
                edit = @Edit(title = "ASN 单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsWarehouse warehouse;

    @EruptField(views = @View(title = "供应商"),
                edit = @Edit(title = "供应商"))
    @Column(length = 200)
    private String supplierName;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AsnStatus")))
    private Integer status = AsnStatus.NEW.code;

    @EruptField(views = @View(title = "预计到货日期"),
                edit = @Edit(title = "预计到货日期",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "asn_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "ASN 明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<WmsAsnItem> items = new ArrayList<>();

    public static class Proxy extends WmsStateDataProxy<WmsAsn> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ AsnStatus.NEW.code, null };
        }
    }
}
