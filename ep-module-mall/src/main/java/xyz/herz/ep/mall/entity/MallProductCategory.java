package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Tree;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTreeType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.enums.MallDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 商品分类(树形)。 */
@Getter @Setter
@Entity
@Table(name = "mall_product_category")
@Erupt(
    name = "商品分类",
    tree = @Tree(id = "id", label = "name", pid = "parentId")
)
public class MallProductCategory extends BaseModel {

    @EruptField(
        views = @View(title = "分类名称"),
        edit = @Edit(title = "分类名称", type = EditType.INPUT, notNull = true,
            inputType = @InputType)
    )
    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private MallProductCategory parent;

    @EruptField(
        edit = @Edit(title = "上级分类", type = EditType.REFERENCE_TREE,
            referenceTreeType = @ReferenceTreeType(id = "id", label = "name"))
    )
    @Transient
    private MallProductCategory parentUi;

    @EruptField(
        views = @View(title = "排序"),
        edit = @Edit(title = "排序", numberType = @NumberType(min = 0))
    )
    private Integer sort = 0;

    @EruptField(
        views = @View(title = "状态"),
        edit = @Edit(title = "状态", notNull = true,
            choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                fetchHandlerParams = "EnableStatus"))
    )
    private Integer status = EnableStatus.ENABLED.code;
}
