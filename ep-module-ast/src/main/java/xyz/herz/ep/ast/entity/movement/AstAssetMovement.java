package xyz.herz.ep.ast.entity.movement;

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
import xyz.herz.ep.ast.enums.AstDictEnums.MovementStatus;
import xyz.herz.ep.ast.enums.AstDictEnums.MovementType;
import xyz.herz.ep.ast.handler.movement.AstMovementLifecycleHandler;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 资产转移单(参考 ERPNext Asset Movement DocType)。
 * <p>状态机:0草稿 → 1已提交(同步更新资产 location/custodian);2已取消为终态。
 * <p>转移类型:位置/保管人/部门/成本中心;来源与目标用快照名称,不强制 REF。
 */
@Getter @Setter
@Entity
@Table(name = "ast_asset_movement")
@Erupt(
    name = "资产转移单",
    power = @Power(importable = true, export = true),
    dataProxy = AstAssetMovement.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = AstMovementLifecycleHandler.CODE_SUBMIT,
            operationHandler = AstMovementLifecycleHandler.class,
            operationParam = { AstMovementLifecycleHandler.CODE_SUBMIT }),
        @RowOperation(title = "取消", code = AstMovementLifecycleHandler.CODE_CANCEL,
            operationHandler = AstMovementLifecycleHandler.class,
            operationParam = { AstMovementLifecycleHandler.CODE_CANCEL })
    }
)
public class AstAssetMovement extends MetaModelVo {

    @EruptField(views = @View(title = "转移单号"),
                edit = @Edit(title = "转移单号", notNull = true, search = @Search))
    @Column(name = "no", length = 50, nullable = false)
    private String no;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @EruptField(views = @View(title = "资产", column = "asset_no"),
                edit = @Edit(title = "资产", type = EditType.REFERENCE_TABLE, notNull = true,
                    referenceTableType = @ReferenceTableType(id = "id", label = "asset_no")))
    private AstAsset asset;

    @EruptField(views = @View(title = "转移类型"),
                edit = @Edit(title = "转移类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MovementType")))
    @Column(name = "movement_type", nullable = false)
    private Integer movementType = MovementType.LOCATION.code;

    @EruptField(views = @View(title = "原位置"),
                edit = @Edit(title = "原位置"))
    @Column(name = "from_location_name", length = 100)
    private String fromLocationName;

    @EruptField(views = @View(title = "新位置"),
                edit = @Edit(title = "新位置"))
    @Column(name = "to_location_name", length = 100)
    private String toLocationName;

    @EruptField(views = @View(title = "原保管人"),
                edit = @Edit(title = "原保管人"))
    @Column(name = "from_custodian_name", length = 100)
    private String fromCustodianName;

    @EruptField(views = @View(title = "新保管人"),
                edit = @Edit(title = "新保管人"))
    @Column(name = "to_custodian_name", length = 100)
    private String toCustodianName;

    @EruptField(views = @View(title = "原成本中心ID"),
                edit = @Edit(title = "原成本中心ID", show = false))
    @Column(name = "from_cost_center_id")
    private Long fromCostCenterId;

    @EruptField(views = @View(title = "新成本中心ID"),
                edit = @Edit(title = "新成本中心ID", show = false))
    @Column(name = "to_cost_center_id")
    private Long toCostCenterId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "MovementStatus")))
    @Column(nullable = false)
    private Integer status = MovementStatus.DRAFT.code;

    @EruptField(views = @View(title = "转移日期"),
                edit = @Edit(title = "转移日期",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "movement_date")
    private LocalDate movementDate;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends AstStateDataProxy<AstAssetMovement> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
