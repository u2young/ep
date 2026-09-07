package xyz.herz.ep.ast.entity.repair;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
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
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.enums.AstDictEnums.RepairStatus;
import xyz.herz.ep.ast.handler.repair.AstRepairLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产维修单(参考 ERPNext Asset Repair DocType)。
 * <p>状态机:0草稿 → 1已完成(维修成本可过账 GL 费用类);2已取消为终态。
 */
@Getter @Setter
@Entity
@Table(name = "ast_asset_repair")
@Erupt(
    name = "资产维修单",
    power = @Power(importable = true, export = true),
    dataProxy = AstAssetRepair.Proxy.class,
    rowOperation = {
        @RowOperation(title = "完成", code = AstRepairLifecycleHandler.CODE_COMPLETE,
            operationHandler = AstRepairLifecycleHandler.class,
            operationParam = { AstRepairLifecycleHandler.CODE_COMPLETE }),
        @RowOperation(title = "取消", code = AstRepairLifecycleHandler.CODE_CANCEL,
            operationHandler = AstRepairLifecycleHandler.class,
            operationParam = { AstRepairLifecycleHandler.CODE_CANCEL })
    }
)
public class AstAssetRepair extends MetaModelVo {

    @EruptField(views = @View(title = "维修单号"),
                edit = @Edit(title = "维修单号", notNull = true, search = @Search))
    @Column(name = "no", length = 50, nullable = false)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @EruptField(views = @View(title = "资产", column = "asset_no"),
                edit = @Edit(title = "资产", type = EditType.REFERENCE_TABLE, notNull = true,
                    referenceTableType = @ReferenceTableType(id = "id", label = "asset_no")))
    private AstAsset asset;

    @EruptField(views = @View(title = "维修日期"),
                edit = @Edit(title = "维修日期",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "repair_date")
    private LocalDate repairDate;

    @EruptField(views = @View(title = "维修成本"),
                edit = @Edit(title = "维修成本", notNull = true))
    @Column(name = "repair_cost", precision = 18, scale = 2, nullable = false)
    private BigDecimal repairCost;

    @EruptField(views = @View(title = "维修人"),
                edit = @Edit(title = "维修人"))
    @Column(name = "repairer_name", length = 100)
    private String repairerName;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "RepairStatus")))
    @Column(nullable = false)
    private Integer status = RepairStatus.DRAFT.code;

    @EruptField(views = @View(title = "描述"),
                edit = @Edit(title = "描述", type = EditType.TEXTAREA))
    @Lob
    @Column(name = "description", columnDefinition = "CLOB")
    private String description;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends AstStateDataProxy<AstAssetRepair> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
