package xyz.herz.ep.fin.entity.account;

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
import xyz.herz.ep.fin.enums.FinDictEnums.AccountType;
import xyz.herz.ep.fin.handler.account.FinAccountToggleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 会计科目(树形,参考 ERPNext Account DocType)。
 * <p>状态机:DISABLED(0) ↔ ENABLED(1),通过行按钮切换,禁止表单直改。
 */
@Getter @Setter
@Entity
@Table(name = "fin_account")
@Erupt(
    name = "会计科目",
    power = @Power(importable = true, export = true),
    dataProxy = FinAccount.Proxy.class,
    rowOperation = {
        @RowOperation(title = "启用", code = FinAccountToggleHandler.ENABLE, icon = "fa fa-check-circle",
            operationHandler = FinAccountToggleHandler.class, operationParam = { FinAccountToggleHandler.ENABLE }),
        @RowOperation(title = "停用", code = FinAccountToggleHandler.DISABLE, icon = "fa fa-ban",
            operationHandler = FinAccountToggleHandler.class, operationParam = { FinAccountToggleHandler.DISABLE })
    }
)
public class FinAccount extends MetaModelVo {

    @EruptField(views = @View(title = "科目编码"),
                edit = @Edit(title = "科目编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @EruptField(views = @View(title = "科目名称"),
                edit = @Edit(title = "科目名称", notNull = true))
    @Column(length = 200, nullable = false)
    private String name;

    @EruptField(views = @View(title = "父科目", column = "name"),
                edit = @Edit(title = "父科目", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FinAccount parent;

    @EruptField(views = @View(title = "科目类型"),
                edit = @Edit(title = "科目类型", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "AccountType")))
    @Column(nullable = false)
    private Integer accountType = AccountType.ASSET.code;

    @EruptField(views = @View(title = "是否组科目"),
                edit = @Edit(title = "是否组科目", desc = "组科目不可直接挂明细凭证"))
    @Column(nullable = false)
    private Boolean isGroup = false;

    @EruptField(views = @View(title = "余额"), edit = @Edit(title = "余额", show = false))
    @Column(precision = 24, scale = 6)
    private BigDecimal balance = BigDecimal.ZERO;

    @EruptField(views = @View(title = "币种"), edit = @Edit(title = "币种"))
    @Column(length = 10)
    private String currency = "CNY";

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = FinEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;

    public static class Proxy extends FinStateDataProxy<FinAccount> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ EnableStatus.ENABLED.code, EnableStatus.DISABLED.code, null };
        }
    }
}
