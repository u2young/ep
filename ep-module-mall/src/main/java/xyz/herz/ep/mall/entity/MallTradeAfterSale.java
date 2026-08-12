package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.core.MallStateDataProxy;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleType;
import xyz.herz.ep.mall.handler.MallAfterSaleAgreeHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleBuyerShipHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleCompleteHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleRejectHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleSellerReceiveHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 售后单。
 * <p>状态机:10 申请 → 20 同意 → 30 买家发货 → 40 卖家收货 → 60 完成;10 → 70 拒绝。
 * 状态字段锁定,只能通过行按钮(同意/拒绝/买家发货/卖家收货/完成退款)变更。
 */
@Getter @Setter
@Entity
@Table(name = "mall_trade_after_sale")
@Erupt(
    name = "售后单",
    power = @Power(importable = true, export = true),
    dataProxy = MallTradeAfterSale.Proxy.class,
    rowOperation = {
        @RowOperation(title = "同意", code = MallAfterSaleAgreeHandler.CODE, icon = "fa fa-check",
            operationHandler = MallAfterSaleAgreeHandler.class, operationParam = { MallAfterSaleAgreeHandler.CODE }),
        @RowOperation(title = "拒绝", code = MallAfterSaleRejectHandler.CODE, icon = "fa fa-ban",
            operationHandler = MallAfterSaleRejectHandler.class, operationParam = { MallAfterSaleRejectHandler.CODE }),
        @RowOperation(title = "买家发货", code = MallAfterSaleBuyerShipHandler.CODE, icon = "fa fa-truck",
            operationHandler = MallAfterSaleBuyerShipHandler.class, operationParam = { MallAfterSaleBuyerShipHandler.CODE }),
        @RowOperation(title = "卖家收货", code = MallAfterSaleSellerReceiveHandler.CODE, icon = "fa fa-inbox",
            operationHandler = MallAfterSaleSellerReceiveHandler.class, operationParam = { MallAfterSaleSellerReceiveHandler.CODE }),
        @RowOperation(title = "完成退款", code = MallAfterSaleCompleteHandler.CODE, icon = "fa fa-money",
            operationHandler = MallAfterSaleCompleteHandler.class, operationParam = { MallAfterSaleCompleteHandler.CODE })
    }
)
public class MallTradeAfterSale extends BaseModel {

    @EruptField(views = @View(title = "售后单号"),
                edit = @Edit(title = "售后单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "订单ID"), edit = @Edit(title = "订单ID"))
    private Long orderId;

    @EruptField(views = @View(title = "订单项ID"), edit = @Edit(title = "订单项ID"))
    private Long orderItemId;

    @EruptField(views = @View(title = "售后类型"),
                edit = @Edit(title = "售后类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AfterSaleType")))
    private Integer type = AfterSaleType.REFUND_ONLY.code;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AfterSaleStatus")))
    private Integer status = AfterSaleStatus.APPLY.code;

    @EruptField(views = @View(title = "退款金额"),
                edit = @Edit(title = "退款金额", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "申请原因"), edit = @Edit(title = "申请原因"))
    @Column(length = 500)
    private String reason;

    /** 锁定 status,仅允许初始态(10 申请/ null)直接写入。 */
    public static class Proxy extends MallStateDataProxy<MallTradeAfterSale> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ AfterSaleStatus.APPLY.code, null };
        }
    }
}
