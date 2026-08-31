package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Layout;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.core.MallStateDataProxy;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.handler.MallOrderCancelHandler;
import xyz.herz.ep.mall.handler.MallOrderConfirmHandler;
import xyz.herz.ep.mall.handler.MallOrderPayHandler;
import xyz.herz.ep.mall.handler.MallOrderShipHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易订单(主表)。
 * <p>状态机:0 待付款 → 10 待发货 → 20 待收货 → 30 已完成;0/10 → 40 已取消。
 * 状态字段锁定,只能通过行按钮(付款/发货/确认收货/取消)变更。
 */
@Getter @Setter
@Entity
@Table(name = "mall_trade_order")
@Erupt(
    name = "交易订单",
    power = @Power(importable = true, export = true),
    layout = @Layout(collapseActionButton = true),
    dataProxy = MallTradeOrder.Proxy.class,
    rowOperation = {
        @RowOperation(title = "付款", code = MallOrderPayHandler.CODE, icon = "fa fa-money",
            operationHandler = MallOrderPayHandler.class, operationParam = { MallOrderPayHandler.CODE }),
        @RowOperation(title = "发货", code = MallOrderShipHandler.CODE, icon = "fa fa-truck",
            operationHandler = MallOrderShipHandler.class, operationParam = { MallOrderShipHandler.CODE }),
        @RowOperation(title = "确认收货", code = MallOrderConfirmHandler.CODE, icon = "fa fa-inbox",
            operationHandler = MallOrderConfirmHandler.class, operationParam = { MallOrderConfirmHandler.CODE }),
        @RowOperation(title = "取消", code = MallOrderCancelHandler.CODE, icon = "fa fa-ban",
            operationHandler = MallOrderCancelHandler.class, operationParam = { MallOrderCancelHandler.CODE })
    }
)
public class MallTradeOrder extends BaseModel {

    @EruptField(views = @View(title = "订单号"),
                edit = @Edit(title = "订单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "用户ID"),
                edit = @Edit(title = "用户ID"))
    private Long userId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "OrderStatus")))
    private Integer status = OrderStatus.UNPAID.code;

    @EruptField(views = @View(title = "订单总金额"),
                edit = @Edit(title = "订单总金额", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "实付金额"),
                edit = @Edit(title = "实付金额", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal payAmount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "支付时间"),
                edit = @Edit(title = "支付时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime payTime;

    @EruptField(views = @View(title = "发货时间"),
                edit = @Edit(title = "发货时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime shipTime;

    @EruptField(views = @View(title = "收货时间"),
                edit = @Edit(title = "收货时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime confirmTime;

    @EruptField(views = @View(title = "取消时间"),
                edit = @Edit(title = "取消时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime cancelTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    @EruptField(edit = @Edit(title = "订单明细", type = EditType.TAB_TABLE_ADD))
    private List<MallTradeOrderItem> items = new ArrayList<>();

    /** 锁定 status,仅允许初始态(0/ null)直接写入;同时回绑子项 order 引用。 */
    public static class Proxy extends MallStateDataProxy<MallTradeOrder> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ OrderStatus.UNPAID.code, null };
        }

        private void bindChildren(MallTradeOrder o) {
            if (o.getItems() != null) {
                for (MallTradeOrderItem it : o.getItems()) {
                    if (it.getOrder() == null) it.setOrder(o);
                }
            }
        }

        @Override public void beforeAdd(MallTradeOrder o) { bindChildren(o); }
        @Override public void beforeUpdate(MallTradeOrder o) { bindChildren(o); super.beforeUpdate(o); }
    }
}
