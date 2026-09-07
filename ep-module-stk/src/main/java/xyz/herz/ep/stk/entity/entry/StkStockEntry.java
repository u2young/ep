package xyz.herz.ep.stk.entity.entry;

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
import xyz.herz.ep.stk.core.StkEnumChoiceFetchHandler;
import xyz.herz.ep.stk.core.StkStateDataProxy;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryStatus;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryType;
import xyz.herz.ep.stk.handler.entry.StkEntryLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存出入库单(参考 ERPNext Stock Entry DocType)。
 * <p>状态机:0草稿/1已提交/2已取消。
 * 5 种类型:入库(MATERIAL_RECEIPT)/出库(MATERIAL_ISSUE)/移库(MATERIAL_TRANSFER)/
 * 生产(MANUFACTURE)/翻包(REPACK)。
 *
 * <p>提交时回写 totalQty/totalAmount = Σ(item.qty/amount);
 * 与既有 ep-module-wms 的 WmsStock(仓位库存快照)共存,本单覆盖 ERPNext 库存专属出入库流水。
 */
@Getter @Setter
@Entity
@Table(name = "stk_entry")
@Erupt(
    name = "库存出入库单",
    power = @Power(importable = true, export = true),
    dataProxy = StkStockEntry.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = StkEntryLifecycleHandler.CODE_SUBMIT,
            operationHandler = StkEntryLifecycleHandler.class),
        @RowOperation(title = "取消", code = StkEntryLifecycleHandler.CODE_CANCEL,
            operationHandler = StkEntryLifecycleHandler.class)
    }
)
public class StkStockEntry extends MetaModelVo {

    @EruptField(views = @View(title = "单号"),
                edit = @Edit(title = "单号", notNull = true, search = @Search))
    @Column(name = "entry_no", length = 50, nullable = false)
    private String entryNo;

    @EruptField(views = @View(title = "类型"),
                edit = @Edit(title = "类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = StkEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EntryType")))
    @Column(name = "entry_type", nullable = false)
    private Integer entryType = EntryType.MATERIAL_RECEIPT.code;

    @EruptField(views = @View(title = "来源仓编码"),
                edit = @Edit(title = "来源仓编码", desc = "出库/移库时填写"))
    @Column(name = "source_warehouse_code", length = 50)
    private String sourceWarehouseCode;

    @EruptField(views = @View(title = "来源仓名称"),
                edit = @Edit(title = "来源仓名称"))
    @Column(name = "source_warehouse_name", length = 100)
    private String sourceWarehouseName;

    @EruptField(views = @View(title = "目标仓编码"),
                edit = @Edit(title = "目标仓编码", desc = "入库/移库时填写"))
    @Column(name = "target_warehouse_code", length = 50)
    private String targetWarehouseCode;

    @EruptField(views = @View(title = "目标仓名称"),
                edit = @Edit(title = "目标仓名称"))
    @Column(name = "target_warehouse_name", length = 100)
    private String targetWarehouseName;

    @EruptField(views = @View(title = "过账日期"),
                edit = @Edit(title = "过账日期", notNull = true))
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @EruptField(views = @View(title = "合计数量"),
                edit = @Edit(title = "合计数量", show = false, desc = "提交时回写"))
    @Column(name = "total_qty", precision = 18, scale = 2)
    private BigDecimal totalQty;

    @EruptField(views = @View(title = "合计金额"),
                edit = @Edit(title = "合计金额", show = false, desc = "提交时回写"))
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @EruptField(views = @View(title = "提交时间"),
                edit = @Edit(title = "提交时间", show = false, desc = "提交时回写"))
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @EruptField(views = @View(title = "操作人"),
                edit = @Edit(title = "操作人"))
    @Column(name = "operator", length = 50)
    private String operator;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = StkEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EntryStatus")))
    @Column(nullable = false)
    private Integer status = EntryStatus.DRAFT.code;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "出入库明细", type = EditType.TAB_TABLE_ADD))
    private List<StkStockEntryItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends StkStateDataProxy<StkStockEntry> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
