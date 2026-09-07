package xyz.herz.ep.pur.entity.quotation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商报价(参考 ERPNext Supplier Quotation DocType)。
 * <p>无状态机,CRUD + 是否采纳标记;来自某询价单的供应商回复,选定后可用来生成采购订单。
 */
@Getter @Setter
@Entity
@Table(name = "pur_supplier_quotation")
@Erupt(name = "供应商报价", power = @Power(importable = true, export = true))
public class PurSupplierQuotation extends MetaModelVo {

    @EruptField(views = @View(title = "报价单号"),
                edit = @Edit(title = "报价单号", notNull = true, search = @Search))
    @Column(name = "quotation_no", length = 50, nullable = false)
    private String quotationNo;

    @EruptField(views = @View(title = "来源询价单"),
                edit = @Edit(title = "来源询价单", search = @Search))
    @Column(name = "rfq_no", length = 50)
    private String rfqNo;

    @EruptField(views = @View(title = "供应商编码"),
                edit = @Edit(title = "供应商编码", notNull = true, search = @Search))
    @Column(name = "supplier_code", length = 50, nullable = false)
    private String supplierCode;

    @EruptField(views = @View(title = "供应商名称"),
                edit = @Edit(title = "供应商名称", notNull = true))
    @Column(name = "supplier_name", length = 100, nullable = false)
    private String supplierName;

    @EruptField(views = @View(title = "报价日期"),
                edit = @Edit(title = "报价日期", notNull = true))
    @Column(name = "quotation_date", nullable = false)
    private LocalDate quotationDate;

    @EruptField(views = @View(title = "有效期至"),
                edit = @Edit(title = "有效期至", notNull = true))
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @EruptField(views = @View(title = "报价总额"),
                edit = @Edit(title = "报价总额", notNull = true))
    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @EruptField(views = @View(title = "是否采纳"),
                edit = @Edit(title = "是否采纳", desc = "选定后用来下采购订单"))
    @Column(name = "is_accepted")
    private Boolean isAccepted = false;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
