package xyz.herz.ep.mp.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.mp.core.MpEnumChoiceFetchHandler;
import xyz.herz.ep.mp.enums.MpDictEnums.EnableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 公众号账号。 */
@Getter @Setter
@Entity
@Table(name = "mp_account")
@Erupt(name = "公众号账号", power = @Power(importable = true, export = true))
public class MpAccount extends BaseModel {

    @EruptField(views = @View(title = "名称"),
                edit = @Edit(title = "名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "AppID"),
                edit = @Edit(title = "AppID", notNull = true, search = @Search))
    @Column(length = 100, nullable = false, unique = true)
    private String appId;

    @EruptField(views = @View(title = "AppSecret"),
                edit = @Edit(title = "AppSecret", notNull = true, inputType = @InputType))
    @Column(length = 100, nullable = false)
    private String appSecret;

    @EruptField(views = @View(title = "Token"),
                edit = @Edit(title = "Token"))
    @Column(length = 100)
    private String token;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
