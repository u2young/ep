package xyz.herz.ep.erp.entity.product;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 产品 SKU(多属性规格明细)。
 *
 * <p>设计约定:
 * <ul>
 *   <li>一个 product (SPU) 至少有一条 SKU;无属性的单品,SKU 与 product 共用同一套信息。</li>
 *   <li>属性值用 {@code specsJson} 保存,如 {@code {"颜色":"红色","尺码":"XL"}};</li>
 *   <li>后续库存、单据都按 SKU 维度(productId + skuId 任意一个都可以定位到具体一条,统一用 skuId 做单据明细)。</li>
 * </ul>
 */
@Getter @Setter
@Entity
@Table(name = "erp_product_sku")
@Erupt(name = "产品SKU")
public class ErpProductSku extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ErpProduct product;

    @EruptField(views = @View(title = "SKU 编码"), edit = @Edit(title = "SKU 编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "规格属性 JSON"),
                edit = @Edit(title = "规格属性 JSON", type = EditType.CODE_EDITOR,
                    desc = "例: {\"颜色\": \"红色\", \"尺码\": \"XL\"}"))
    @Column(columnDefinition = "CLOB")
    @Lob
    private String specsJson;

    @EruptField(views = @View(title = "规格描述(显示)"),
                edit = @Edit(title = "规格描述(显示)", desc = "拼接后的人类可读文本,列表展示用"))
    @Column(length = 300)
    private String specsText;

    @EruptField(views = @View(title = "默认销售价"),
                edit = @Edit(title = "默认销售价", numberType = @NumberType(min = 0)))
    private BigDecimal salePrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "最近采购价"),
                edit = @Edit(title = "最近采购价", numberType = @NumberType(min = 0)))
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "标准成本价"),
                edit = @Edit(title = "标准成本价", numberType = @NumberType(min = 0)))
    private BigDecimal costPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "安全库存"),
                edit = @Edit(title = "安全库存", numberType = @NumberType(min = 0)))
    private BigDecimal safetyStock = BigDecimal.ZERO;

    @EruptField(views = @View(title = "启用"), edit = @Edit(title = "启用"))
    private Boolean enabled = true;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
