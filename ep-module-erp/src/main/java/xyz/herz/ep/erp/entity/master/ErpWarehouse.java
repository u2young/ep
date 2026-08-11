package xyz.herz.ep.erp.entity.master;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.InputType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.jpa.model.MetaModelVo;

import xyz.herz.ep.erp.core.ErpEnumChoiceFetchHandler;
import xyz.herz.ep.erp.core.ErpStateDataProxy;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;
import xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 仓库档案。 */
@Getter @Setter
@Entity
@Table(name = "erp_warehouse")
@Erupt(
    name = "仓库档案",
    dataProxy = ErpWarehouse.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = ErpMasterToggleHandler.ENABLE, icon = "fa fa-check", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = ErpMasterToggleHandler.DISABLE, icon = "fa fa-ban", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.DISABLE }),
        @RowOperation(title = "设为默认仓", code = ErpMasterToggleHandler.DEFAULT_WH, icon = "fa fa-star", operationHandler = xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler.class, operationParam = { ErpMasterToggleHandler.DEFAULT_WH })
    }
)
public class ErpWarehouse extends MetaModelVo {

    @EruptField(views = @View(title = "仓库名称"),
                edit = @Edit(title = "仓库名称", notNull = true))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "仓库编码"),
                edit = @Edit(title = "仓库编码", notNull = true))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "详细地址"),
                edit = @Edit(title = "详细地址", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String address;

    @EruptField(views = @View(title = "联系人"),
                edit = @Edit(title = "联系人"))
    @Column(length = 50)
    private String contact;

    @EruptField(views = @View(title = "联系电话"),
                edit = @Edit(title = "联系电话", inputType = @InputType))
    @Column(length = 50)
    private String mobile;

    @EruptField(views = @View(title = "默认仓库"),
                edit = @Edit(title = "默认仓库",
                    desc = "新建单据时默认选中,一个租户只能有一个默认仓(按钮设默认会清其他)"))
    private Boolean defaultFlag = false;

    @EruptField(views = @View(title = "排序"),
                edit = @Edit(title = "排序", numberType = @NumberType(min = 0)))
    private Integer sort = 0;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    desc = "停用后单据中不再可选",
                    choiceType = @ChoiceType(fetchHandler = ErpEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends ErpStateDataProxy<ErpWarehouse> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
