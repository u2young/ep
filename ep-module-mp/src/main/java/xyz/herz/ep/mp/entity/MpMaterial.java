package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 素材管理。permanent=true 永久,false 临时(3 天)。 */
@Getter @Setter
@Entity
@Table(name = "mp_material")
@Erupt(name = "素材管理", power = @Power(importable = true, export = true))
public class MpMaterial extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "素材类型", sortable = true),
                edit = @Edit(title = "素材类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MaterialType")))
    @Column(length = 32, nullable = false)
    private String type;

    @EruptField(views = @View(title = "媒体ID"), edit = @Edit(title = "媒体ID"))
    @Column(length = 128)
    private String mediaId;

    @EruptField(views = @View(title = "URL"), edit = @Edit(title = "URL"))
    @Column(length = 1024)
    private String url;

    @EruptField(views = @View(title = "文件名"), edit = @Edit(title = "文件名"))
    @Column(length = 255)
    private String name;

    @EruptField(views = @View(title = "是否永久"),
                edit = @Edit(title = "是否永久", desc = "true=永久,false=临时(3天)"))
    private Boolean permanent = Boolean.TRUE;

    @EruptField(views = @View(title = "创建时间", sortable = true),
                edit = @Edit(title = "创建时间", type = EditType.DATE,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    private LocalDateTime createTime;
}
