package xyz.herz.ep.erp.entity.stock;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存流水(只增不改)。biz_type+biz_id 做幂等去重。 */
@Getter @Setter
@Entity
@Table(name = "erp_stock_record",
    indexes = {
        @Index(name = "idx_stock_rec_biz", columnList = "biz_type, biz_id"),
        @Index(name = "idx_stock_rec_pw", columnList = "product_id, sku_id, warehouse_id")
    })
@Erupt(name = "库存流水",
    dataProxy = xyz.herz.ep.erp.core.ErpStockBalanceProxy.class)
public class ErpStockRecord extends MetaModelVo {

    @EruptField(views = @View(title = "业务类型"),
                edit = @Edit(title = "业务类型", show = false,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "StockBizType")))
    @Column(name = "biz_type", nullable = false)
    private Integer bizType;

    @EruptField(views = @View(title = "业务单据ID"),
                edit = @Edit(title = "业务单据ID", show = false))
    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    @EruptField(views = @View(title = "业务单据号"),
                edit = @Edit(title = "业务单据号", show = false))
    @Column(length = 60)
    private String bizNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", show = false, type = EditType.REFERENCE_TABLE))
    private ErpProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id")
    @EruptField(views = @View(title = "SKU", column = "code"),
                edit = @Edit(title = "SKU", show = false, type = EditType.REFERENCE_TABLE))
    private ErpProductSku sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", show = false, type = EditType.REFERENCE_TABLE))
    private ErpWarehouse warehouse;

    @EruptField(views = @View(title = "批号"),
                edit = @Edit(title = "批号", show = false))
    @Column(length = 100, nullable = false)
    private String batchNo = "-";

    @EruptField(views = @View(title = "变动数量(正入/负出)"))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "变动后余额"))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal afterQty = BigDecimal.ZERO;

    @EruptField(views = @View(title = "变动时间"),
                edit = @Edit(title = "变动时间", show = false))
    @Column(nullable = false)
    private LocalDateTime eventTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", show = false, type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
