package xyz.herz.ep.sal.entity.quotation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler;
import xyz.herz.ep.sal.core.SalStateDataProxy;
import xyz.herz.ep.sal.enums.SalDictEnums.OrderType;
import xyz.herz.ep.sal.enums.SalDictEnums.QuotationStatus;

/**
 * 报价单(参考 ERPNext Quotation DocType)。
 * <p>状态机:0草稿/1已提交/2已取消/3已拒绝/4报价接受。
 * 与既有模块的 PurSupplierQuotation 区分:含完整状态机 + 订单类型关联。
 */
@Getter @Setter
@Entity
@Table(name = "sal_quotation")
@Erupt(name = "报价单", power = @Power(importable = true, export = true))
public class SalQuotation extends MetaModelVo {

    @EruptField(views = @View(title = "报价单号"),
            edit = @Edit(title = "报价单号", notNull = true, search = @Search))
    @Column(name = "quotation_no", length = 50, nullable = false)
    private String quotationNo;

    @EruptField(views = @View(title = "客户编码"),
            edit = @Edit(title = "客户编码", notNull = true, search = @Search))
    @Column(name = "customer_code", length = 50, nullable = false)
    private String customerCode;

    @EruptField(views = @View(title = "客户名称"),
            edit = @Edit(title = "客户名称", notNull = true))
    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @EruptField(views = @View(title = "订单类型"),
            edit = @Edit(title = "订单类型", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "OrderType")))
    @Column(name = "order_type", nullable = false)
    private Integer orderType = OrderType.SALES.code;

    @EruptField(views = @View(title = "预计成交金额"),
            edit = @Edit(title = "预计成交金额", notNull = true))
    @Column(name = "expected_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal expectedAmount;

    @EruptField(views = @View(title = "有效期至"),
            edit = @Edit(title = "有效期至", notNull = true))
    @Column(name = "valid_date", nullable = false)
    private LocalDate validDate;

    @EruptField(views = @View(title = "状态"),
            edit = @Edit(title = "状态", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "QuotationStatus")))
    @Column(name = "status", nullable = false)
    private Integer status = QuotationStatus.DRAFT.code;

    @EruptField(views = @View(title = "创建人"),
            edit = @Edit(title = "创建人"))
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @EruptField(views = @View(title = "行明细"),
            edit = @Edit(title = "行明细", type = EditType.TAB_TABLE_ADD))
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    private List<SalQuotationItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SalStateDataProxy<SalQuotation> {
        @Override protected String stateFieldName() { return "status"; }
    }
}