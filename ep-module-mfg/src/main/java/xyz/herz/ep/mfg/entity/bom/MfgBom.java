package xyz.herz.ep.mfg.entity.bom;

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
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.enums.MfgDictEnums.EnableStatus;
import xyz.herz.ep.mfg.handler.master.MfgMasterToggleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BOM 单头(参考 ERPNext BOM DocType)。
 * <p>状态机:DISABLED(0) ↔ ENABLED(1),主数据允许行按钮切换。
 * <p>多级 BOM 通过 isDefault 标记默认工艺;items 为 BOM 明细(MfgBomItem)。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_bom")
@Erupt(
    name = "BOM 工艺清单",
    power = @Power(importable = true, export = true),
    dataProxy = MfgBom.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = MfgMasterToggleHandler.ENABLE, icon = "fa fa-check-circle",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = MfgMasterToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.DISABLE })
    }
)
public class MfgBom extends MetaModelVo {

    @EruptField(views = @View(title = "BOM 编号"),
                edit = @Edit(title = "BOM 编号", notNull = true, search = @Search))
    @Column(length = 60, nullable = false, unique = true)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "product_id")
    @EruptField(views = @View(title = "产品", column = "name"),
                edit = @Edit(title = "产品", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ErpProduct product;

    @EruptField(views = @View(title = "生效"),
                edit = @Edit(title = "生效", desc = "是否当前生效的 BOM"))
    @Column(nullable = false)
    private Boolean isActive = true;

    @EruptField(views = @View(title = "默认"),
                edit = @Edit(title = "默认", desc = "是否产品的默认 BOM"))
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @EruptField(views = @View(title = "是否含工序"),
                edit = @Edit(title = "是否含工序", desc = "含工序时工单按 MfgOperation 派工"))
    @Column(name = "with_operations", nullable = false)
    private Boolean withOperations = false;

    @EruptField(views = @View(title = "总成本"), edit = @Edit(title = "总成本", show = false))
    @Column(name = "total_cost", precision = 24, scale = 6)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 1000)
    private String remark;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bom_id")
    @EruptField(
        views = @View(title = "明细条数"),
        edit = @Edit(title = "BOM 明细", type = EditType.TAB_TABLE_ADD)
    )
    private List<MfgBomItem> items = new ArrayList<>();

    public static class Proxy extends MfgStateDataProxy<MfgBom> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
