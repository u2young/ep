package xyz.herz.ep.fin.entity.costcenter;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.fin.core.FinEnumChoiceFetchHandler;
import xyz.herz.ep.fin.core.FinStateDataProxy;
import xyz.herz.ep.fin.enums.FinDictEnums.EnableStatus;
import xyz.herz.ep.fin.handler.costcenter.FinCostCenterToggleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 成本中心(树形,参考 ERPNext Cost Center DocType)。
 * <p>状态机:DISABLED(0) ↔ ENABLED(1),通过行按钮切换,禁止表单直改。
 */
@Getter @Setter
@Entity
@Table(name = "fin_cost_center")
@Erupt(
    name = "成本中心",
    power = @Power(importable = true, export = true),
    dataProxy = FinCostCenter.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = FinCostCenterToggleHandler.ENABLE, icon = "fa fa-check-circle",
            operationHandler = FinCostCenterToggleHandler.class, operationParam = { FinCostCenterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = FinCostCenterToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = FinCostCenterToggleHandler.class, operationParam = { FinCostCenterToggleHandler.DISABLE })
    }
)
public class FinCostCenter extends MetaModelVo {

    @EruptField(views = @View(title = "编码"),
                edit = @Edit(title = "编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "名称"),
                edit = @Edit(title = "名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "父成本中心", column = "name"),
                edit = @Edit(title = "父成本中心", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FinCostCenter parent;

    @EruptField(views = @View(title = "是否组"), edit = @Edit(title = "是否组"))
    @Column(nullable = false)
    private Boolean isGroup = false;

    @EruptField(views = @View(title = "年度预算分配"),
                edit = @Edit(title = "年度预算分配", numberType = @NumberType(min = 0)))
    @Column(precision = 24, scale = 6)
    private BigDecimal budgetAllocated = BigDecimal.ZERO;

    @EruptField(views = @View(title = "实际归集"), edit = @Edit(title = "实际归集", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal budgetActual = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    public static class Proxy extends FinStateDataProxy<FinCostCenter> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
