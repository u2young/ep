package xyz.herz.ep.mfg.entity.workstation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.enums.MfgDictEnums.EnableStatus;
import xyz.herz.ep.mfg.handler.master.MfgMasterToggleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 工作中心(参考 ERPNext Workstation DocType)。
 * <p>状态机:DISABLED(0) ↔ ENABLED(1),主数据允许行按钮切换,故 Proxy 放松直改白名单。
 * <p>工序定义(MfgOperation) REF 本表;派工单产能计算引用 productionCapacity + hourRate。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_workstation")
@Erupt(
    name = "工作中心",
    power = @Power(importable = true, export = true),
    dataProxy = MfgWorkstation.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = MfgMasterToggleHandler.ENABLE, icon = "fa fa-check-circle",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = MfgMasterToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.DISABLE })
    }
)
public class MfgWorkstation extends MetaModelVo {

    @EruptField(views = @View(title = "编码"),
                edit = @Edit(title = "编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "名称"),
                edit = @Edit(title = "名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "产能"),
                edit = @Edit(title = "产能", desc = "同时可作业数", notNull = true,
                    numberType = @NumberType(min = 1)))
    @Column(nullable = false)
    private Integer productionCapacity = 1;

    @EruptField(views = @View(title = "小时费率"),
                edit = @Edit(title = "小时费率", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "hour_rate", precision = 24, scale = 6)
    private BigDecimal hourRate = BigDecimal.ZERO;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = MfgEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends MfgStateDataProxy<MfgWorkstation> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
