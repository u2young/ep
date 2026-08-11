package xyz.herz.ep.erp.entity.purchase;

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
import xyz.herz.ep.erp.entity.master.ErpSupplier;
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
 * 采购入库单。
 * <p>
 * 状态机和订单对称(DRAFT → APPROVED(=触发库存入库+写应付) / UNAPPROVE(=冲销)),但有仓库差异:
 * 订单是「默认入库仓库」,入库单支持「按行 warehouse_id 分配」,但这里 P0 简化为整单一仓
 * (与订单 warehouse 字段一致,需要拆分仓库时开多张入库单即可)。
 */
@Getter @Setter
@Entity
@Table(name = "erp_purchase_in")
@Erupt(
    name = "采购入库单",
    power = @Power(importable = true, export = true),
    dataProxy = ErpPurchaseIn.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审核(入库)", code = ErpDocAuditHandler.CODE_APPROVE, icon = "fa fa-check-circle"),
        @RowOperation(title = "反审核(冲销出库)", code = ErpDocAuditHandler.CODE_UNAPPROVE, icon = "fa fa-undo"),
        @RowOperation(title = "作废", code = ErpDocAuditHandler.CODE_VOID, icon = "fa fa-trash", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_VOID })
    }
)
public class ErpPurchaseIn extends MetaModelVo {

    @EruptField(views = @View(title = "入库单号"),
                edit = @Edit(title = "入库单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "入库时间"),
                edit = @Edit(title = "入库时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(nullable = false)
    private LocalDateTime inTime = LocalDateTime.now();

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AuditStatus")))
    private Integer status = AuditStatus.DRAFT.code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    @EruptField(views = @View(title = "供应商", column = "name"),
                edit = @Edit(title = "供应商", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpSupplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @EruptField(views = @View(title = "结算账户", column = "name"),
                edit = @Edit(title = "结算账户", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @EruptField(views = @View(title = "关联采购订单", column = "no"),
                edit = @Edit(title = "关联采购订单(可选)", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "no")))
    private ErpPurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "入库仓库", column = "name"),
                edit = @Edit(title = "入库仓库", notNull = true, type = EditType.REFERENCE_TABLE,
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

    @EruptField(views = @View(title = "其他费用"), edit = @Edit(title = "其他费用",
        desc = "关税/运费等,不计入商品金额但计入应付/成本分摊后续扩展", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal otherPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "应付总金额"),
                edit = @Edit(title = "应付总金额(商品额+税额-优惠额+其他费用)", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已付款金额"),
                edit = @Edit(title = "已付款金额(付款单核销回写,P0预留)", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal paymentPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "附件"), edit = @Edit(title = "附件", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String fileUrl;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "in_id")
    @EruptField(edit = @Edit(title = "入库明细", type = EditType.TAB_TABLE_ADD))
    private List<ErpPurchaseInItem> items = new ArrayList<>();

    public static class Proxy extends ErpStateDataProxy<ErpPurchaseIn> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ AuditStatus.DRAFT.code, null };
        }
    }
}
