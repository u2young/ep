package xyz.herz.ep.erp.entity.master;

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
import xyz.erupt.jpa.model.MetaModelVo;

import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 产品分类(树形)。 */
@Getter @Setter
@Entity
@Table(name = "erp_product_category")
@Erupt(
    name = "产品分类",
    tree = @Tree(id = "id", label = "name", pid = "parentId")
)
public class ErpProductCategory extends MetaModelVo {

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
    private ErpProductCategory parent;

    @EruptField(
        edit = @Edit(title = "上级分类", type = EditType.REFERENCE_TREE,
            referenceTreeType = @ReferenceTreeType(id = "id", label = "name"))
    )
    @Transient
    private ErpProductCategory parentUi;

    @EruptField(
        views = @View(title = "排序"),
        edit = @Edit(title = "排序", numberType = @NumberType(min = 0))
    )
    private Integer sort = 0;

    @EruptField(
        views = @View(title = "状态"),
        edit = @Edit(title = "状态", notNull = true,
            choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                fetchHandlerParams = "EnableStatus"))
    )
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(
        views = @View(title = "备注"),
        edit = @Edit(title = "备注", type = EditType.TEXTAREA)
    )
    @Column(length = 500)
    private String remark;
}
