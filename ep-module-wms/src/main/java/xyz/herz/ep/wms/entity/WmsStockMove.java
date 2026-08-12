package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 库存流水(append-only,只增不改)。
 * <p>记录每次上架/拣货/移库的库位级变动。biz_type 为字符串(如 PUTAWAY/PICK/MOVE)。
 */
@Getter @Setter
@Entity
@Table(name = "wms_stock_move",
    indexes = {
        @Index(name = "idx_move_sku", columnList = "sku_code"),
        @Index(name = "idx_move_loc", columnList = "from_location_id, to_location_id")
    })
@Erupt(name = "库存流水")
public class WmsStockMove extends BaseModel {

    @EruptField(views = @View(title = "源库位ID"),
                edit = @Edit(title = "源库位ID", show = false))
    @Column(name = "from_location_id")
    private Long fromLocationId;

    @EruptField(views = @View(title = "目标库位ID"),
                edit = @Edit(title = "目标库位ID", show = false))
    @Column(name = "to_location_id")
    private Long toLocationId;

    @EruptField(views = @View(title = "SKU 编码"),
                edit = @Edit(title = "SKU 编码", show = false))
    @Column(name = "sku_code", length = 100, nullable = false)
    private String skuCode;

    @EruptField(views = @View(title = "变动数量"),
                edit = @Edit(title = "变动数量", show = false))
    @Column(name = "qty", nullable = false)
    private Integer qty;

    @EruptField(views = @View(title = "业务类型"),
                edit = @Edit(title = "业务类型", show = false))
    @Column(name = "biz_type", length = 50, nullable = false)
    private String bizType;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", show = false))
    @Column(name = "remark", length = 500)
    private String remark;
}
