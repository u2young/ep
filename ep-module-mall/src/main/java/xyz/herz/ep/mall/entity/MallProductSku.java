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

/**
 * 商品 SKU(规格明细)。
 * <p>{@link #stock} 为只读字段(由交易/库存联动维护,不允许后台直接编辑)。
 */
@Getter @Setter
@Entity
@Table(name = "mall_product_sku")
@Erupt(name = "商品SKU")
public class MallProductSku extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private MallProductSpu product;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "规格"),
                edit = @Edit(title = "规格"))
    @Column(length = 300)
    private String specs;

    @EruptField(views = @View(title = "价格"),
                edit = @Edit(title = "价格", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal price = BigDecimal.ZERO;

    @EruptField(views = @View(title = "库存"),
                edit = @Edit(title = "库存", show = false))
    private Integer stock = 0;
}
