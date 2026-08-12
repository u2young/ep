package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 粉丝标签。userIds 为预留字段(@Lob),后续批量打标签使用。 */
@Getter @Setter
@Entity
@Table(name = "mp_user_tag")
@Erupt(name = "粉丝标签", power = @Power(importable = true, export = true))
public class MpUserTag extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @EruptField(views = @View(title = "公众号", column = "name"),
                edit = @Edit(title = "公众号", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType(id = "id", label = "name")))
    private MpAccount account;

    @EruptField(views = @View(title = "标签名称"),
                edit = @Edit(title = "标签名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @Lob
    @EruptField(views = @View(title = "粉丝ID列表(预留)"),
                edit = @Edit(title = "粉丝ID列表(预留)", type = EditType.TEXTAREA,
                    desc = "预留字段,后续批量打标签使用,逗号分隔的粉丝ID"))
    private String userIds;
}
