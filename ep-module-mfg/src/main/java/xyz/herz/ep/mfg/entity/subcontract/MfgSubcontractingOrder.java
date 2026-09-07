package xyz.herz.ep.mfg.entity.subcontract;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.SubcontractingStatus;
import xyz.herz.ep.mfg.handler.subcontract.MfgSubcontractingLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 委外单(参考 ERPNext Subcontracting Order DocType)。
 * <p>状态机:DRAFT(0) → ORDERED(1) → PARTIAL_RECEIVED(2) → FULL_RECEIVED(3) → SETTLED(4) / CANCELLED(5)。
 * 只有 DRAFT 允许表单直改;下单/收货/结算/取消走行按钮。
 * <p>委外发料/收货通过 MfgStockEntry(ttype=SUBCONTRACT_ISSUE/RECEIPT)联动库存。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_subcontracting_order")
@Erupt(
    name = "委外单",
    power = @Power(importable = true, export = true),
    dataProxy = MfgSubcontractingOrder.Proxy.class,
    rowOperation = {
        @RowOperation(title = "下单", code = MfgSubcontractingLifecycleHandler.CODE_SUBMIT, icon = "fa fa-paper-plane",
            operationHandler = MfgSubcontractingLifecycleHandler.class, operationParam = { MfgSubcontractingLifecycleHandler.CODE_SUBMIT }),
        @RowOperation(title = "收货", code = MfgSubcontractingLifecycleHandler.CODE_RECEIVE, icon = "fa fa-inbox",
            operationHandler = MfgSubcontractingLifecycleHandler.class, operationParam = { MfgSubcontractingLifecycleHandler.CODE_RECEIVE }),
        @RowOperation(title = "结算", code = MfgSubcontractingLifecycleHandler.CODE_SETTLE, icon = "fa fa-calculator",
            operationHandler = MfgSubcontractingLifecycleHandler.class, operationParam = { MfgSubcontractingLifecycleHandler.CODE_SETTLE }),
        @RowOperation(title = "取消", code = MfgSubcontractingLifecycleHandler.CODE_CANCEL, icon = "fa fa-undo",
            operationHandler = MfgSubcontractingLifecycleHandler.class, operationParam = { MfgSubcontractingLifecycleHandler.CODE_CANCEL })
    }
)
public class MfgSubcontractingOrder extends MetaModelVo {

    @EruptField(views = @View(title = "委外单号"),
                edit = @Edit(title = "委外单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supplier_id")
    @EruptField(views = @View(title = "供应商", column = "name"),
                edit = @Edit(title = "供应商", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpSupplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "work_order_id")
    @EruptField(views = @View(title = "工单", column = "no"),
                edit = @Edit(title = "工单", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private MfgWorkOrder workOrder;

    @EruptField(views = @View(title = "委外数量"),
                edit = @Edit(title = "委外数量", notNull = true, numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已收货数"), edit = @Edit(title = "已收货数", show = false))
    @Column(name = "received_qty", precision = 24, scale = 6)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "加工费率"),
                edit = @Edit(title = "加工费率", notNull = true, numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal rate = BigDecimal.ZERO;

    @EruptField(views = @View(title = "总金额"), edit = @Edit(title = "总金额", show = false))
    @Column(name = "total_amount", precision = 24, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "SubcontractingStatus")))
    @Column(nullable = false)
    private Integer status = SubcontractingStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends MfgStateDataProxy<MfgSubcontractingOrder> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ SubcontractingStatus.DRAFT.code, null };
        }
    }
}
