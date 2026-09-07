package xyz.herz.ep.mfg.entity.operation;

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
import xyz.herz.ep.mfg.core.MfgEnumChoiceFetchHandler;
import xyz.herz.ep.mfg.core.MfgStateDataProxy;
import xyz.herz.ep.mfg.entity.workstation.MfgWorkstation;
import xyz.herz.ep.mfg.enums.MfgDictEnums.EnableStatus;
import xyz.herz.ep.mfg.handler.master.MfgMasterToggleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 工序定义(参考 ERPNext Operation DocType)。
 * <p>状态机:DISABLED(0) ↔ ENABLED(1),主数据允许行按钮切换。
 * <p>字段命名规避 H2 关键字:序列号用 seqNo(不用 level/sequence),
 * 工时用 timeInMins(不用 time)。
 */
@Getter @Setter
@Entity
@Table(name = "mfg_operation")
@Erupt(
    name = "工序定义",
    power = @Power(importable = true, export = true),
    dataProxy = MfgOperation.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = MfgMasterToggleHandler.ENABLE, icon = "fa fa-check-circle",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = MfgMasterToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = MfgMasterToggleHandler.class, operationParam = { MfgMasterToggleHandler.DISABLE })
    }
)
public class MfgOperation extends MetaModelVo {

    @EruptField(views = @View(title = "编码"),
                edit = @Edit(title = "编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false, unique = true)
    private String no;

    @EruptField(views = @View(title = "工序名称"),
                edit = @Edit(title = "工序名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "workstation_id")
    @EruptField(views = @View(title = "工作中心", column = "name"),
                edit = @Edit(title = "工作中心", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private MfgWorkstation workstation;

    @EruptField(views = @View(title = "序号"),
                edit = @Edit(title = "序号", desc = "工艺顺序(小在前)", notNull = true,
                    numberType = @NumberType(min = 1)))
    @Column(name = "seq_no", nullable = false)
    private Integer seqNo = 1;

    @EruptField(views = @View(title = "工时(分)"),
                edit = @Edit(title = "工时(分)", notNull = true,
                    numberType = @NumberType(min = 0)))
    @Column(name = "time_in_mins", precision = 24, scale = 6)
    private BigDecimal timeInMins = BigDecimal.ZERO;

    @EruptField(views = @View(title = "小时费率"),
                edit = @Edit(title = "小时费率", numberType = @NumberType(min = 0)))
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

    public static class Proxy extends MfgStateDataProxy<MfgOperation> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
