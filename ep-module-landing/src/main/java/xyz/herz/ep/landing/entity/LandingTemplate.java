package xyz.herz.ep.landing.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.DragSort;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.landing.core.LandingEnumChoiceFetchHandler;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 落地页预设模板。
 * <p>存储 amis schema JSON,管理员可基于模板创建落地页。
 * 内置分类:空白页/留资表单/产品介绍/海报单页/活动报名。
 */
@Getter @Setter
@Entity
@Table(name = "landing_template")
@Erupt(name = "落地页模板",
    power = @Power(importable = true, export = true, copy = true),
    dragSort = @DragSort(field = "sort"))
public class LandingTemplate extends BaseModel {

    @EruptField(views = @View(title = "模板名称"),
                edit = @Edit(title = "模板名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "排序", sortable = true),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "分类"),
                edit = @Edit(title = "分类", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "TemplateCategory")))
    @Column(nullable = false)
    private Integer category;

    @EruptField(views = @View(title = "amis Schema"),
                edit = @Edit(title = "amis Schema(JSON)", type = EditType.TEXTAREA))
    @Lob @Column(columnDefinition = "TEXT")
    private String schema;

    @EruptField(views = @View(title = "缩略图"),
                edit = @Edit(title = "缩略图URL"))
    @Column(name = "thumb_url", length = 500)
    private String thumbUrl;

    @EruptField(views = @View(title = "启用"),
                edit = @Edit(title = "启用", search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer enabled = EnableStatus.ENABLED.code;
}
