package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mall.core.MallEnumChoiceFetchHandler;
import xyz.herz.ep.mall.enums.MallDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 商品品牌。 */
@Getter @Setter
@Entity
@Table(name = "mall_product_brand")
@Erupt(name = "商品品牌")
public class MallProductBrand extends BaseModel {

    @EruptField(views = @View(title = "品牌名称"),
                edit = @Edit(title = "品牌名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "品牌 LOGO"),
                edit = @Edit(title = "品牌 LOGO", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String logo;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MallEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;
}
