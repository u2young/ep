package xyz.herz.ep.sal.entity.deliverynote;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler;
import xyz.herz.ep.sal.core.SalStateDataProxy;
import xyz.herz.ep.sal.enums.SalDictEnums.DeliveryNoteStatus;
import xyz.herz.ep.sal.handler.deliverynote.SalDeliveryNoteLifecycleHandler;

/**
 * 送货单(参考 ERPNext Delivery Note DocType)。
 * <p>状态机:0草稿/1已提交/2已暂停/3已取消/4已发货。
 * 关联销售订单;含总数量/总金额。
 */
@Getter @Setter
@Entity
@Table(name = "sal_delivery_note")
@Erupt(
    name = "送货单",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "提交", code = SalDeliveryNoteLifecycleHandler.CODE_SUBMIT,
            operationHandler = SalDeliveryNoteLifecycleHandler.class),
        @RowOperation(title = "发货", code = SalDeliveryNoteLifecycleHandler.CODE_DELIVER,
            operationHandler = SalDeliveryNoteLifecycleHandler.class),
        @RowOperation(title = "取消", code = SalDeliveryNoteLifecycleHandler.CODE_CANCEL,
            operationHandler = SalDeliveryNoteLifecycleHandler.class)
    }
)
public class SalDeliveryNote extends MetaModelVo {

    @EruptField(views = @View(title = "送货单号"),
            edit = @Edit(title = "送货单号", notNull = true, search = @Search))
    @Column(name = "delivery_no", length = 50, nullable = false)
    private String deliveryNo;

    @EruptField(views = @View(title = "来源订单号"),
            edit = @Edit(title = "来源订单号", search = @Search))
    @Column(name = "source_order_no", length = 50)
    private String sourceOrderNo;

    @EruptField(views = @View(title = "客户编码"),
            edit = @Edit(title = "客户编码", notNull = true, search = @Search))
    @Column(name = "customer_code", length = 50, nullable = false)
    private String customerCode;

    @EruptField(views = @View(title = "客户名称"),
            edit = @Edit(title = "客户名称", notNull = true))
    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @EruptField(views = @View(title = "送货日期"),
            edit = @Edit(title = "送货日期"))
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @EruptField(views = @View(title = "收货地址"),
            edit = @Edit(title = "收货地址"))
    @Column(length = 200)
    private String address;

    @EruptField(views = @View(title = "总数量"),
            edit = @Edit(title = "总数量", show = false, desc = "发货时回写合计"))
    @Column(name = "total_qty", precision = 18, scale = 2)
    private BigDecimal totalQty;

    @EruptField(views = @View(title = "总金额"),
            edit = @Edit(title = "总金额", show = false, desc = "发货时回写合计"))
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @EruptField(views = @View(title = "状态"),
            edit = @Edit(title = "状态", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "DeliveryNoteStatus")))
    @Column(name = "status", nullable = false)
    private Integer status = DeliveryNoteStatus.DRAFT.code;

    @EruptField(views = @View(title = "明细"),
            edit = @Edit(title = "明细", type = EditType.TAB_TABLE_ADD))
    @OneToMany(mappedBy = "deliveryNote", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    private List<SalDeliveryNoteItem> items;

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SalStateDataProxy<SalDeliveryNote> {
        @Override protected String stateFieldName() { return "status"; }
    }
}