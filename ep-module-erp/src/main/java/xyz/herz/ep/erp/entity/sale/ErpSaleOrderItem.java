package xyz.herz.ep.erp.entity.sale;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "erp_sale_order_item")
@Erupt(name = "销售订单明细")
public class ErpSaleOrderItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ErpSaleOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProduct product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id")
    @EruptField(views = @View(title = "SKU", column = "code"),
                edit = @Edit(title = "SKU", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private ErpProductSku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    @EruptField(views = @View(title = "单位", column = "name"),
                edit = @Edit(title = "单位", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProductUnit unit;

    @EruptField(views = @View(title = "销售数量"),
                edit = @Edit(title = "销售数量", notNull = true, numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6, nullable = false)
    private BigDecimal count = BigDecimal.ZERO;

    @EruptField(views = @View(title = "销售单价"),
                edit = @Edit(title = "销售单价", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal productPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "税率%"),
                edit = @Edit(title = "税率%", numberType = @NumberType(min = 0, max = 100)))
    @Column(precision = 10, scale = 4)
    private BigDecimal taxPercent = BigDecimal.ZERO;

    @EruptField(views = @View(title = "税额"), edit = @Edit(title = "税额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal taxPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "行金额(含税)"), edit = @Edit(title = "行金额(含税)", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已出库数量"), edit = @Edit(title = "已出库数量", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal outCount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "客户退货数量"), edit = @Edit(title = "客户退货数量", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal returnCount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 300)
    private String remark;
}
