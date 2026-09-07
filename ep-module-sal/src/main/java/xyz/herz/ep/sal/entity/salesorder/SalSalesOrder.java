package xyz.herz.ep.sal.entity.salesorder;

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
import java.time.LocalDateTime;
import java.util.List;

import xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler;
import xyz.herz.ep.sal.core.SalStateDataProxy;
import xyz.herz.ep.sal.enums.SalDictEnums.SalesOrderStatus;
import xyz.herz.ep.sal.handler.salesorder.SalSalesOrderLifecycleHandler;

/**
 * 销售订单(参考 ERPNext Sales Order DocType)。
 * <p>状态机:0草稿/1已提交/2已暂停/3已取消/4已完成。
 * 关联报价单来源;含订单总金额/已发货数量/已完成状态。
 */
@Getter @Setter
@Entity
@Table(name = "sal_sales_order")
@Erupt(
    name = "销售订单",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "提交", code = SalSalesOrderLifecycleHandler.CODE_SUBMIT,
            operationHandler = SalSalesOrderLifecycleHandler.class),
        @RowOperation(title = "完成", code = SalSalesOrderLifecycleHandler.CODE_COMPLETE,
            operationHandler = SalSalesOrderLifecycleHandler.class),
        @RowOperation(title = "暂停", code = SalSalesOrderLifecycleHandler.CODE_HOLD,
            operationHandler = SalSalesOrderLifecycleHandler.class),
        @RowOperation(title = "取消", code = SalSalesOrderLifecycleHandler.CODE_CANCEL,
            operationHandler = SalSalesOrderLifecycleHandler.class)
    }
)
public class SalSalesOrder extends MetaModelVo {

    @EruptField(views = @View(title = "订单号"),
            edit = @Edit(title = "订单号", notNull = true, search = @Search))
    @Column(name = "order_no", length = 50, nullable = false)
    private String orderNo;

    @EruptField(views = @View(title = "来源报价单"),
            edit = @Edit(title = "来源报价单", search = @Search))
    @Column(name = "quotation_no", length = 50)
    private String quotationNo;

    @EruptField(views = @View(title = "客户编码"),
            edit = @Edit(title = "客户编码", notNull = true, search = @Search))
    @Column(name = "customer_code", length = 50, nullable = false)
    private String customerCode;

    @EruptField(views = @View(title = "客户名称"),
            edit = @Edit(title = "客户名称", notNull = true))
    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @EruptField(views = @View(title = "销售员"),
            edit = @Edit(title = "销售员"))
    @Column(name = "sales_person_code", length = 50)
    private String salesPersonCode;

    @EruptField(views = @View(title = "订单日期"),
            edit = @Edit(title = "订单日期"))
    @Column(name = "order_date")
    private LocalDate orderDate;

    @EruptField(views = @View(title = "交货日期"),
            edit = @Edit(title = "交货日期"))
    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @EruptField(views = @View(title = "订单总金额"),
            edit = @Edit(title = "订单总金额", notNull = true))
    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @EruptField(views = @View(title = "已发货数量"),
            edit = @Edit(title = "已发货数量", show = false, desc = "发货时回写合计"))
    @Column(name = "delivered_qty", precision = 18, scale = 2)
    private BigDecimal deliveredQty;

    @EruptField(views = @View(title = "已发货金额"),
            edit = @Edit(title = "已发货金额", show = false, desc = "发货时回写合计"))
    @Column(name = "delivered_amount", precision = 18, scale = 2)
    private BigDecimal deliveredAmount;

    @EruptField(views = @View(title = "提交时间"),
            edit = @Edit(title = "提交时间", show = false))
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @EruptField(views = @View(title = "完成时间"),
            edit = @Edit(title = "完成时间", show = false))
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @EruptField(views = @View(title = "状态"),
            edit = @Edit(title = "状态", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "SalesOrderStatus")))
    @Column(name = "status", nullable = false)
    private Integer status = SalesOrderStatus.DRAFT.code;

    @EruptField(views = @View(title = "订单明细"),
            edit = @Edit(title = "订单明细", type = EditType.TAB_TABLE_ADD))
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    private List<SalSalesOrderItem> items;

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SalStateDataProxy<SalSalesOrder> {
        @Override protected String stateFieldName() { return "status"; }
    }
}