package xyz.herz.ep.erp.entity.master;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;

import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 供应商(采购往来单位)。 */
@Getter @Setter
@Entity
@Table(name = "erp_supplier")
@Erupt(name = "供应商档案")
public class ErpSupplier extends MetaModelVo {

    @EruptField(views = @View(title = "供应商名称"),
                edit = @Edit(title = "供应商名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "供应商编码"),
                edit = @Edit(title = "供应商编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "联系人"),
                edit = @Edit(title = "联系人"))
    @Column(length = 50)
    private String contact;

    @EruptField(views = @View(title = "手机"), edit = @Edit(title = "手机"))
    @Column(length = 50)
    private String mobile;

    @EruptField(views = @View(title = "电话"), edit = @Edit(title = "电话", inputType = @InputType))
    @Column(length = 50)
    private String telephone;

    @EruptField(views = @View(title = "邮箱"),
                edit = @Edit(title = "邮箱", inputType = @InputType))
    @Column(length = 100)
    private String email;

    @EruptField(views = @View(title = "地址"), edit = @Edit(title = "地址", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String address;

    @EruptField(views = @View(title = "纳税人识别号"),
                edit = @Edit(title = "纳税人识别号"))
    @Column(length = 50)
    private String taxNo;

    @EruptField(views = @View(title = "默认税率%"),
                edit = @Edit(title = "默认税率%", numberType = @NumberType(min = 0, max = 100)))
    private BigDecimal taxPercent = BigDecimal.ZERO;

    @EruptField(views = @View(title = "开户行"), edit = @Edit(title = "开户行"))
    @Column(length = 100)
    private String bankName;

    @EruptField(views = @View(title = "银行账号"), edit = @Edit(title = "银行账号"))
    @Column(length = 50)
    private String bankAccount;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;
}
