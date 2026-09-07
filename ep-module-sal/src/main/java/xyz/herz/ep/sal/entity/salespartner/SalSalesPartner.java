package xyz.herz.ep.sal.entity.salespartner;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import jakarta.persistence.*;

import xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler;
import xyz.herz.ep.sal.enums.SalDictEnums.EnableStatus;

/**
 * 销售伙伴(参考 ERPNext Sales Partner DocType)。
 * <p>主数据:代理商编码/名称/状态/联系人/电话/邮箱/合作期限/备注。
 */
@Getter @Setter
@Entity
@Table(name = "sal_sales_partner")
@Erupt(name = "销售伙伴", power = @Power(importable = true, export = true))
public class SalSalesPartner extends MetaModelVo {

    @EruptField(views = @View(title = "代理商编码"),
            edit = @Edit(title = "代理商编码", notNull = true, search = @Search))
    @Column(name = "partner_code", length = 50, nullable = false)
    private String partnerCode;

    @EruptField(views = @View(title = "代理商名称"),
            edit = @Edit(title = "代理商名称", notNull = true))
    @Column(name = "partner_name", length = 100, nullable = false)
    private String partnerName;

    @EruptField(views = @View(title = "状态"),
            edit = @Edit(title = "状态", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "EnableStatus")))
    @Column(name = "status", nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "联系人"),
            edit = @Edit(title = "联系人"))
    @Column(name = "contact_name", length = 100)
    private String contactName;

    @EruptField(views = @View(title = "联系电话"),
            edit = @Edit(title = "联系电话"))
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @EruptField(views = @View(title = "联系邮箱"),
            edit = @Edit(title = "联系邮箱"))
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @EruptField(views = @View(title = "合作起始"),
            edit = @Edit(title = "合作起始"))
    @Column(name = "partner_since", length = 50)
    private String partnerSince;

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}