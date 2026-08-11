package xyz.herz.ep.erp.entity.sale;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.core.ErpStateDataProxy;
import xyz.herz.ep.erp.entity.master.ErpAccount;
import xyz.herz.ep.erp.entity.master.ErpCustomer;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.enums.ErpDictEnums.AuditStatus;
import xyz.herz.ep.erp.handler.document.ErpDocAuditHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售出库单。
 * <p>
 * 状态机对称:草稿→(审核=扣库存+生成应收) / 反审=冲销(加库存+红字应收) / 作废。
 * 整单一仓,支持批次号。
 */
@Getter @Setter
@Entity
@Table(name = "erp_sale_out")
@Erupt(
    name = "销售出库单",
    power = @Power(importable = true, export = true),
    dataProxy = ErpSaleOut.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审核(出库)", code = ErpDocAuditHandler.CODE_APPROVE, icon = "fa fa-check-circle"),
        @RowOperation(title = "反审核(冲销入库)", code = ErpDocAuditHandler.CODE_UNAPPROVE, icon = "fa fa-undo"),
        @RowOperation(title = "作废", code = ErpDocAuditHandler.CODE_VOID, icon = "fa fa-trash", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_VOID })
    }
)
public class ErpSaleOut extends MetaModelVo {

    @EruptField(views = @View(title = "出库单号"),
                edit = @Edit(title = "出库单号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "出库时间"),
                edit = @Edit(title = "出库时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(nullable = false)
    private LocalDateTime outTime = LocalDateTime.now();

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AuditStatus")))
    private Integer status = AuditStatus.DRAFT.code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @EruptField(views = @View(title = "客户", column = "name"),
                edit = @Edit(title = "客户", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @EruptField(views = @View(title = "结算账户", column = "name"),
                edit = @Edit(title = "结算账户", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @EruptField(views = @View(title = "关联销售订单", column = "no"),
                edit = @Edit(title = "关联销售订单(可选)", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private ErpSaleOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "出库仓库", column = "name"),
                edit = @Edit(title = "出库仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse warehouse;

    @EruptField(views = @View(title = "合计数量"), edit = @Edit(title = "合计数量", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalCount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "商品金额"), edit = @Edit(title = "商品金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalProductPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "税额"), edit = @Edit(title = "税额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalTaxPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "优惠率%"), edit = @Edit(title = "优惠率%",
        numberType = @NumberType(min = 0, max = 100)))
    @Column(precision = 10, scale = 4)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @EruptField(views = @View(title = "优惠额"), edit = @Edit(title = "优惠额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal discountPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "运费/其他费用"), edit = @Edit(title = "运费/其他费用", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal otherPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "应收总金额"), edit = @Edit(title = "应收总金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已收款金额"), edit = @Edit(title = "已收款金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal receiptPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "附件"), edit = @Edit(title = "附件", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String fileUrl;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "out_id")
    @EruptField(edit = @Edit(title = "出库明细", type = EditType.TAB_TABLE_ADD))
    private List<ErpSaleOutItem> items = new ArrayList<>();

    public static class Proxy extends ErpStateDataProxy<ErpSaleOut> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ AuditStatus.DRAFT.code, null };
        }
    }
}
