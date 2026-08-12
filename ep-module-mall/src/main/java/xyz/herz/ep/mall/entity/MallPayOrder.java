package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.enums.MallDictEnums.PayStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单(订单付款时联动生成,状态由支付回调维护)。
 */
@Getter @Setter
@Entity
@Table(name = "mall_pay_order")
@Erupt(
    name = "支付单",
    power = @Power(importable = true, export = true)
)
public class MallPayOrder extends BaseModel {

    @EruptField(views = @View(title = "支付单号"),
                edit = @Edit(title = "支付单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "订单ID"), edit = @Edit(title = "订单ID"))
    private Long orderId;

    @EruptField(views = @View(title = "支付金额"),
                edit = @Edit(title = "支付金额", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PayStatus")))
    private Integer status = PayStatus.UNPAID.code;

    @EruptField(views = @View(title = "支付时间"),
                edit = @Edit(title = "支付时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime payTime;

    @EruptField(views = @View(title = "支付方式"), edit = @Edit(title = "支付方式"))
    @Column(length = 50)
    private String payType;
}
