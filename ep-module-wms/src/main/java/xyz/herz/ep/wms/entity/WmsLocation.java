package xyz.herz.ep.wms.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.wms.core.WmsEnumChoiceFetchHandler;
import xyz.herz.ep.wms.enums.WmsDictEnums.LocationStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 库位档案。归属于库区。 */
@Getter @Setter
@Entity
@Table(name = "wms_location")
@Erupt(name = "库位档案")
public class WmsLocation extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    @EruptField(views = @View(title = "库区", column = "name"),
                edit = @Edit(title = "库区", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private WmsZone zone;

    @EruptField(views = @View(title = "库位名称"),
                edit = @Edit(title = "库位名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "库位编码"),
                edit = @Edit(title = "库位编码", notNull = true))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = WmsEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "LocationStatus")))
    private Integer status = LocationStatus.IDLE.code;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;
}
