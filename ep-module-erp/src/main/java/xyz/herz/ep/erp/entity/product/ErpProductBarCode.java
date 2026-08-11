package xyz.herz.ep.erp.entity.product;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 产品多条码(同一个 SKU 可能有多个条码,比如国际条码/内部码/箱码等)。
 * 设计约定:product 1 ↔ N barCode。
 */
@Getter @Setter
@Entity
@Table(name = "erp_product_barcode")
@Erupt(name = "产品条码")
public class ErpProductBarCode extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private xyz.herz.ep.erp.entity.product.ErpProduct product;

    @EruptField(
        views = @View(title = "条码类型"),
        edit = @Edit(title = "条码类型",
            desc = "EAN13 / UPC / CODE128 / 内部码 / 箱码 / 其他,文本保存即可")
    )
    @Column(length = 30)
    private String type;

    @EruptField(views = @View(title = "条码号"),
                edit = @Edit(title = "条码号", notNull = true, inputType = @InputType))
    @Column(length = 100, nullable = false)
    private String code;

    @EruptField(views = @View(title = "主条码"),
                edit = @Edit(title = "主条码", desc = "扫码出库/入库默认匹配主条码"))
    private Boolean primary = false;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 200)
    private String remark;
}
