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

/** 库区档案。归属于仓库。 */
@Getter @Setter
@Entity
@Table(name = "wms_zone")
@Erupt(name = "库区档案")
public class WmsZone extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "仓库", column = "name"),
                edit = @Edit(title = "仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsWarehouse warehouse;

    @EruptField(views = @View(title = "库区名称"),
                edit = @Edit(title = "库区名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "库区编码"),
                edit = @Edit(title = "库区编码", notNull = true))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;
}
