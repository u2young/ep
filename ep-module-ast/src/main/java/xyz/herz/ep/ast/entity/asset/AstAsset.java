package xyz.herz.ep.ast.entity.asset;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.fun.DataProxy;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.ast.core.AstEnumChoiceFetchHandler;
import xyz.herz.ep.ast.core.AstStateDataProxy;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;
import xyz.herz.ep.ast.entity.depreciation.AstDepreciationSchedule;
import xyz.herz.ep.ast.entity.location.AstLocation;
import xyz.herz.ep.ast.enums.AstDictEnums.AssetStatus;
import xyz.herz.ep.ast.enums.AstDictEnums.DepreciationMethod;
import xyz.herz.ep.ast.handler.asset.AstAssetLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 固定资产主档(参考 ERPNext Asset DocType)。
 * <p>状态机:0草稿 → 1可用(提交) → 2部分折旧 → 3完全折旧;4出售/5报废为终态。
 * <p>折旧引擎:depreciationMethod 决定计算方式,由 AstDepreciationCalculator 计提;
 * 折旧计划表{@link AstDepreciationSchedule}为子表,每期一条记录。
 *
 * <p>跨模块集成(I-ast):category REF {@link AstAssetCategory}(optional),
 * GL 过账科目用 category 上的 accountCode 快照,不强制 REF FinAccount;
 * costCenter/custodian 用快照 id+name,不 REF UPMS。
 */
@Getter @Setter
@Entity
@Table(name = "ast_asset")
@Erupt(
    name = "固定资产",
    power = @Power(importable = true, export = true),
    dataProxy = AstAsset.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = AstAssetLifecycleHandler.CODE_COMMIT,
            operationHandler = AstAssetLifecycleHandler.class,
            operationParam = { AstAssetLifecycleHandler.CODE_COMMIT }),
        @RowOperation(title = "计提折旧", code = AstAssetLifecycleHandler.CODE_DEPRECIATE,
            operationHandler = AstAssetLifecycleHandler.class,
            operationParam = { AstAssetLifecycleHandler.CODE_DEPRECIATE }),
        @RowOperation(title = "出售", code = AstAssetLifecycleHandler.CODE_SELL,
            operationHandler = AstAssetLifecycleHandler.class,
            operationParam = { AstAssetLifecycleHandler.CODE_SELL }),
        @RowOperation(title = "报废", code = AstAssetLifecycleHandler.CODE_SCRAP,
            operationHandler = AstAssetLifecycleHandler.class,
            operationParam = { AstAssetLifecycleHandler.CODE_SCRAP })
    }
)
public class AstAsset extends MetaModelVo {

    @EruptField(views = @View(title = "资产编号"),
                edit = @Edit(title = "资产编号", notNull = true, search = @Search))
    @Column(name = "asset_no", length = 50, nullable = false)
    private String assetNo;

    @EruptField(views = @View(title = "资产名称"),
                edit = @Edit(title = "资产名称", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    @EruptField(views = @View(title = "类别", column = "name"),
                edit = @Edit(title = "类别", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private AstAssetCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "location_id", nullable = true)
    @EruptField(views = @View(title = "位置", column = "name"),
                edit = @Edit(title = "位置", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private AstLocation location;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AssetStatus")))
    @Column(nullable = false)
    private Integer status = AssetStatus.DRAFT.code;

    @EruptField(views = @View(title = "购入日期"),
                edit = @Edit(title = "购入日期",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @EruptField(views = @View(title = "采购成本"),
                edit = @Edit(title = "采购成本(原值)", notNull = true))
    @Column(name = "purchase_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal purchaseAmount;

    @EruptField(views = @View(title = "当前净值"),
                edit = @Edit(title = "当前净值(账面价值)", show = false))
    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    @EruptField(views = @View(title = "残值"),
                edit = @Edit(title = "残值"))
    @Column(name = "salvage_value", precision = 18, scale = 2)
    private BigDecimal salvageValue;

    @EruptField(views = @View(title = "使用年限(月)"),
                edit = @Edit(title = "使用年限(月)", notNull = true))
    @Column(name = "useful_life_months", nullable = false)
    private Integer usefulLifeMonths;

    @EruptField(views = @View(title = "折旧方法"),
                edit = @Edit(title = "折旧方法", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "DepreciationMethod")))
    @Column(name = "depreciation_method", nullable = false)
    private Integer depreciationMethod = DepreciationMethod.STRAIGHT_LINE.code;

    @EruptField(views = @View(title = "折旧率(%)"),
                edit = @Edit(title = "折旧率(%)", desc = "余额递减法用,直线法可留空"))
    @Column(name = "depreciation_rate", precision = 10, scale = 4)
    private BigDecimal depreciationRate;

    @EruptField(views = @View(title = "累计折旧"),
                edit = @Edit(title = "累计折旧", show = false))
    @Column(name = "total_depreciation", precision = 18, scale = 2)
    private BigDecimal totalDepreciation;

    @EruptField(views = @View(title = "已折旧期数"),
                edit = @Edit(title = "已折旧期数", show = false))
    @Column(name = "depreciated_periods")
    private Integer depreciatedPeriods = 0;

    @EruptField(views = @View(title = "成本中心ID"),
                edit = @Edit(title = "成本中心ID", show = false))
    @Column(name = "cost_center_id")
    private Long costCenterId;

    @EruptField(views = @View(title = "保管人"),
                edit = @Edit(title = "保管人"))
    @Column(name = "custodian_name", length = 100)
    private String custodianName;

    @EruptField(views = @View(title = "保管人ID"),
                edit = @Edit(title = "保管人ID", show = false))
    @Column(name = "custodian_id")
    private Long custodianId;

    @EruptField(views = @View(title = "最近折旧凭证ID"),
                edit = @Edit(title = "最近折旧凭证ID", show = false))
    @Column(name = "last_journal_id")
    private Long lastJournalId;

    @EruptField(views = @View(title = "描述"),
                edit = @Edit(title = "描述", type = EditType.TEXTAREA))
    @Lob
    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    // =================== 折旧计划子表 ===================
    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "折旧计划", type = EditType.TAB_TABLE_ADD))
    private List<AstDepreciationSchedule> depreciationSchedules = new ArrayList<>();

    public static class Proxy extends AstStateDataProxy<AstAsset> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
