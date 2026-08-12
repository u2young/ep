package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存余额表(四态)。
 * <p>唯一键 (location_id, sku_code, batch_no);未启用批次时 batch_no='-'。
 * 四态:可用 available / 锁定 locked / 在途 in_transit / 冻结 frozen。
 * <p>本表由 Handler(上架完成/拣货完成)统一维护,不允许表单直改。
 */
@Getter @Setter
@Entity
@Table(name = "wms_stock",
    uniqueConstraints = @UniqueConstraint(columnNames = {"location_id", "sku_code", "batch_no"}))
@Erupt(name = "库存余额")
public class WmsStock extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    @EruptField(views = @View(title = "库位", column = "code"),
                edit = @Edit(title = "库位", show = false, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "code")))
    private WmsLocation location;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", show = false))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "SKU 名称"),
                edit = @Edit(title = "SKU 名称", show = false))
    @Column(name = "sku_name", length = 200)
    private String skuName;

    @EruptField(views = @View(title = "批号"),
                edit = @Edit(title = "批号", show = false))
    @Column(name = "batch_no", length = 100, nullable = false)
    private String batchNo = "-";

    @EruptField(views = @View(title = "可用数量"),
                edit = @Edit(title = "可用数量", show = false, numberType = @NumberType(min = 0)))
    @Column(name = "available_qty", nullable = false)
    private Integer availableQty = 0;

    @EruptField(views = @View(title = "锁定数量"),
                edit = @Edit(title = "锁定数量", show = false, numberType = @NumberType(min = 0)))
    @Column(name = "locked_qty", nullable = false)
    private Integer lockedQty = 0;

    @EruptField(views = @View(title = "在途数量"),
                edit = @Edit(title = "在途数量", show = false, numberType = @NumberType(min = 0)))
    @Column(name = "in_transit_qty", nullable = false)
    private Integer inTransitQty = 0;

    @EruptField(views = @View(title = "冻结数量"),
                edit = @Edit(title = "冻结数量", show = false, numberType = @NumberType(min = 0)))
    @Column(name = "frozen_qty", nullable = false)
    private Integer frozenQty = 0;
}
