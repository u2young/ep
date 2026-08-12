package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 订单明细(下单时刻的商品快照)。 */
@Getter @Setter
@Entity
@Table(name = "mall_trade_order_item")
@Erupt(name = "订单明细")
public class MallTradeOrderItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private MallTradeOrder order;

    @EruptField(views = @View(title = "SKU ID"), edit = @Edit(title = "SKU ID"))
    private Long skuId;

    @EruptField(views = @View(title = "商品名称"), edit = @Edit(title = "商品名称"))
    @Column(length = 255)
    private String skuName;

    @EruptField(views = @View(title = "商品图片"), edit = @Edit(title = "商品图片"))
    @Column(length = 500)
    private String skuPic;

    @EruptField(views = @View(title = "单价"),
                edit = @Edit(title = "单价", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal skuPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "规格"), edit = @Edit(title = "规格"))
    @Column(length = 300)
    private String skuSpec;

    @EruptField(views = @View(title = "购买数量"),
                edit = @Edit(title = "购买数量", notNull = true, numberType = @NumberType(min = 0)))
    private Integer quantity = 0;

    @EruptField(views = @View(title = "锁定库存"),
                edit = @Edit(title = "锁定库存", show = false))
    private Integer lockStock = 0;
}
