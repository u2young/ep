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
 * 采购订单。
 * <p>不影响库存,仅做「下单/回写 in_count」。实际变更库存由 采购入库(ErpPurchaseIn)触发。
 * <p>状态机:Draft → APPROVED(可生成入库单) → CLOSED(终止执行,可选)/VOIDED(作废)。
 */
@Getter @Setter
@Entity
@Table(name = "erp_purchase_order")
@Erupt(
    name = "采购订单",
    power = @Power(importable = true, export = true),
    dataProxy = ErpPurchaseOrder.Proxy.class,
    rowOperation = {
        @RowOperation(title = "审核", code = ErpDocAuditHandler.CODE_APPROVE, icon = "fa fa-check-circle", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_APPROVE }),
        @RowOperation(title = "反审核", code = ErpDocAuditHandler.CODE_UNAPPROVE, icon = "fa fa-undo", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_UNAPPROVE }),
        @RowOperation(title = "关闭", code = ErpDocAuditHandler.CODE_CLOSE, icon = "fa fa-lock", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_CLOSE }),
        @RowOperation(title = "作废", code = ErpDocAuditHandler.CODE_VOID, icon = "fa fa-trash", operationHandler = xyz.herz.ep.erp.handler.document.ErpDocAuditHandler.class, operationParam = { ErpDocAuditHandler.CODE_VOID })
    }
)
public class ErpPurchaseOrder extends MetaModelVo {

    @EruptField(views = @View(title = "订单号"),
                edit = @Edit(title = "订单号", notNull = true))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "下单时间"),
                edit = @Edit(title = "下单时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(nullable = false)
    private LocalDateTime orderTime = LocalDateTime.now();

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, desc = "草稿→审核→(反审/关闭/作废)",
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    @EruptField(views = @View(title = "入库仓库", column = "name"),
                edit = @Edit(title = "入库仓库", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpWarehouse warehouse;

    // === 汇总 ===
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

    @EruptField(views = @View(title = "订单总金额"), edit = @Edit(title = "订单总金额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @EruptField(views = @View(title = "定金"), edit = @Edit(title = "定金", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal depositPrice = BigDecimal.ZERO;

    // === 执行进度(派生,只读) ===
    @EruptField(views = @View(title = "已入库数量"), edit = @Edit(title = "已入库数量", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal inCount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "已退货数量"), edit = @Edit(title = "已退货数量", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal returnCount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "附件"),
                edit = @Edit(title = "附件", type = EditType.ATTACHMENT))
    @Column(length = 500)
    private String fileUrl;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    // === 明细 ===
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "订单明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<ErpPurchaseOrderItem> items = new ArrayList<>();

    /** 状态字段锁定表单直改,只能走 4 个行按钮。 */
    public static class Proxy extends ErpStateDataProxy<ErpPurchaseOrder> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ AuditStatus.DRAFT.code, null };
        }
    }
}
