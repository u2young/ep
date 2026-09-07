package xyz.herz.ep.pur.entity.receipt;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pur.core.PurEnumChoiceFetchHandler;
import xyz.herz.ep.pur.core.PurStateDataProxy;
import xyz.herz.ep.pur.enums.PurDictEnums.ReceiptStatus;
import xyz.herz.ep.pur.handler.receipt.PurReceiptLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购收货单(参考 ERPNext Purchase Receipt DocType)。
 * <p>状态机:0草稿/1已提交/2已收货/3已取消。
 * 收货时回写 receivedAt + receivedQty;与既有 ep-module-erp 的 ErpPurchaseOrder 共存,
 * 用于独立覆盖 ERPNext 采购专属「请购→询价→报价→收货」完整流程。
 */
@Getter @Setter
@Entity
@Table(name = "pur_receipt")
@Erupt(
    name = "采购收货单",
    power = @Power(importable = true, export = true),
    dataProxy = PurPurchaseReceipt.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = PurReceiptLifecycleHandler.CODE_SUBMIT,
            operationHandler = PurReceiptLifecycleHandler.class),
        @RowOperation(title = "收货", code = PurReceiptLifecycleHandler.CODE_RECEIVE,
            operationHandler = PurReceiptLifecycleHandler.class),
        @RowOperation(title = "取消", code = PurReceiptLifecycleHandler.CODE_CANCEL,
            operationHandler = PurReceiptLifecycleHandler.class)
    }
)
public class PurPurchaseReceipt extends MetaModelVo {

    @EruptField(views = @View(title = "收货单号"),
                edit = @Edit(title = "收货单号", notNull = true, search = @Search))
    @Column(name = "receipt_no", length = 50, nullable = false)
    private String receiptNo;

    @EruptField(views = @View(title = "供应商编码"),
                edit = @Edit(title = "供应商编码", notNull = true, search = @Search))
    @Column(name = "supplier_code", length = 50, nullable = false)
    private String supplierCode;

    @EruptField(views = @View(title = "供应商名称"),
                edit = @Edit(title = "供应商名称", notNull = true))
    @Column(name = "supplier_name", length = 100, nullable = false)
    private String supplierName;

    @EruptField(views = @View(title = "来源报价单"),
                edit = @Edit(title = "来源报价单", search = @Search, desc = "可空:直接收货"))
    @Column(name = "source_quotation_no", length = 50)
    private String sourceQuotationNo;

    @EruptField(views = @View(title = "收货日期"),
                edit = @Edit(title = "收货日期"))
    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @EruptField(views = @View(title = "收货数量"),
                edit = @Edit(title = "收货数量", show = false, desc = "收货时回写合计"))
    @Column(name = "received_qty", precision = 18, scale = 2)
    private BigDecimal receivedQty;

    @EruptField(views = @View(title = "收货金额"),
                edit = @Edit(title = "收货金额", show = false, desc = "收货时回写合计"))
    @Column(name = "received_amount", precision = 18, scale = 2)
    private BigDecimal receivedAmount;

    @EruptField(views = @View(title = "收货时间"),
                edit = @Edit(title = "收货时间", show = false, desc = "收货时回写"))
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @EruptField(views = @View(title = "收货人"),
                edit = @Edit(title = "收货人"))
    @Column(name = "receiver", length = 50)
    private String receiver;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PurEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "ReceiptStatus")))
    @Column(nullable = false)
    private Integer status = ReceiptStatus.DRAFT.code;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "收货明细", type = EditType.TAB_TABLE_ADD))
    private List<PurReceiptItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends PurStateDataProxy<PurPurchaseReceipt> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
