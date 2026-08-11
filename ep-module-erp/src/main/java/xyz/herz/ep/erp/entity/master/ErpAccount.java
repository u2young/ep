package xyz.herz.ep.erp.entity.master;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;

import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 结算账户(现金/银行/微信等,收付款单的目标账户)。 */
@Getter @Setter
@Entity
@Table(name = "erp_account")
@Erupt(name = "结算账户")
public class ErpAccount extends MetaModelVo {

    @EruptField(views = @View(title = "账户名称"),
                edit = @Edit(title = "账户名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "账户编码/卡号"),
                edit = @Edit(title = "账户编码/卡号", notNull = true, inputType = @InputType))
    @Column(length = 50, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "默认账户"),
                edit = @Edit(title = "默认账户", desc = "收付款默认带出(一个租户一个)"))
    private Boolean defaultFlag = false;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
