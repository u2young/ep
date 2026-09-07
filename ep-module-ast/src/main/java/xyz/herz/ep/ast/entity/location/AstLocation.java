package xyz.herz.ep.ast.entity.location;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.ast.core.AstEnumChoiceFetchHandler;
import xyz.herz.ep.ast.enums.AstDictEnums.EnableStatus;

import jakarta.persistence.*;

/**
 * 资产位置(参考 ERPNext Location DocType)。
 * <p>放置地点层级(可选 parent 自引用),资产转移时变更 location。
 */
@Getter @Setter
@Entity
@Table(name = "ast_location")
@Erupt(name = "资产位置", power = @Power(importable = true, export = true))
public class AstLocation extends MetaModelVo {

    @EruptField(views = @View(title = "位置编码"),
                edit = @Edit(title = "位置编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "位置名称"),
                edit = @Edit(title = "位置名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_id", nullable = true)
    @EruptField(views = @View(title = "上级位置", column = "name"),
                edit = @Edit(title = "上级位置", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private AstLocation parent;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
