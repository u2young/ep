package xyz.herz.ep.erp.entity.document;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.erp.entity.product.ErpProductSku;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "erp_stock_in_item")
@Erupt(name = "其他入库明细")
public class ErpStockInItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_id")
    private ErpStockIn doc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    @EruptField(views = @View(title = "SKU", column = "code"),
                edit = @Edit(title = "SKU", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private ErpProductSku sku;

    @EruptField(views = @View(title = "入库数量"),
                edit = @Edit(title = "入库数量", notNull = true, numberType = @NumberType(min = 0)))
    @Column(nullable = false)
    private Integer qty = 0;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 300)
    private String remark;
}
