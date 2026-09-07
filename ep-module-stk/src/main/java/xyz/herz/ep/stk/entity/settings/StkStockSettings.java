package xyz.herz.ep.stk.entity.settings;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.BoolType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.stk.core.StkEnumChoiceFetchHandler;
import xyz.herz.ep.stk.enums.StkDictEnums.EnableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 库存配置(单例,参考 ERPNext Stock Settings)。
 * <p>启动时由 StkStockSettingsInitializer 幂等插入;业务方 findFirstByOrderByIdAsc 取唯一配置。
 */
@Getter @Setter
@Entity
@Table(name = "stk_settings")
@Erupt(name = "库存配置", power = @Power(edit = false))
public class StkStockSettings extends MetaModelVo {

    @EruptField(views = @View(title = "默认仓库编码"),
                edit = @Edit(title = "默认仓库编码"))
    @Column(name = "default_warehouse_code", length = 50)
    private String defaultWarehouseCode;

    @EruptField(views = @View(title = "默认仓库名称"))
    @Column(name = "default_warehouse_name", length = 100)
    private String defaultWarehouseName;

    @EruptField(views = @View(title = "是否启用批次管理"),
                edit = @Edit(title = "是否启用批次管理",
                    type = xyz.erupt.annotation.sub_field.EditType.BOOLEAN,
                    boolType = @BoolType(trueText = "是", falseText = "否")))
    @Column(name = "batch_enabled", nullable = false)
    private Boolean batchEnabled = false;

    @EruptField(views = @View(title = "是否启用序列号管理"),
                edit = @Edit(title = "是否启用序列号管理",
                    type = xyz.erupt.annotation.sub_field.EditType.BOOLEAN,
                    boolType = @BoolType(trueText = "是", falseText = "否")))
    @Column(name = "serial_no_enabled", nullable = false)
    private Boolean serialNoEnabled = false;

    @EruptField(views = @View(title = "默认库龄预警天数"),
                edit = @Edit(title = "默认库龄预警天数", desc = "超过此天数提示库龄预警"))
    @Column(name = "stock_age_warning_days")
    private Integer stockAgeWarningDays = 90;

    @EruptField(views = @View(title = "是否启用"),
                edit = @Edit(title = "是否启用", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = StkEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
