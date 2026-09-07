package xyz.herz.ep.ast.entity.category;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.ast.core.AstEnumChoiceFetchHandler;
import xyz.herz.ep.ast.enums.AstDictEnums.DepreciationMethod;
import xyz.herz.ep.ast.enums.AstDictEnums.EnableStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 资产类别(参考 ERPNext Asset Category DocType)。
 * <p>定义折旧方法/残值率/会计科目映射,资产创建时从类别继承默认值。
 * <p>会计科目用 account_code 字符串快照(不强制 REF FinAccount,避免 H2 fixture 顺序耦合)。
 */
@Getter @Setter
@Entity
@Table(name = "ast_asset_category")
@Erupt(name = "资产类别", power = @Power(importable = true, export = true))
public class AstAssetCategory extends MetaModelVo {

    @EruptField(views = @View(title = "类别编码"),
                edit = @Edit(title = "类别编码", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String code;

    @EruptField(views = @View(title = "类别名称"),
                edit = @Edit(title = "类别名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "折旧方法"),
                edit = @Edit(title = "折旧方法", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "DepreciationMethod")))
    @Column(nullable = false)
    private Integer depreciationMethod = DepreciationMethod.STRAIGHT_LINE.code;

    @EruptField(views = @View(title = "折旧率(%)"),
                edit = @Edit(title = "折旧率(%)", desc = "年折旧率,直线法可留空按年限算"))
    @Column(precision = 10, scale = 4)
    private BigDecimal depreciationRate;

    @EruptField(views = @View(title = "残值率(%)"),
                edit = @Edit(title = "残值率(%)", desc = "如 5 表示残值占原值 5%"))
    @Column(precision = 5, scale = 2)
    private BigDecimal salvageRate;

    @EruptField(views = @View(title = "默认使用年限(月)"),
                edit = @Edit(title = "默认使用年限(月)"))
    @Column
    private Integer usefulLifeMonths;

    @EruptField(views = @View(title = "固定资产科目编码"),
                edit = @Edit(title = "固定资产科目编码", desc = "FinAccount.code 快照"))
    @Column(name = "fixed_asset_account_code", length = 50)
    private String fixedAssetAccountCode;

    @EruptField(views = @View(title = "累计折旧科目编码"),
                edit = @Edit(title = "累计折旧科目编码"))
    @Column(name = "accumulated_dep_account_code", length = 50)
    private String accumulatedDepreciationAccountCode;

    @EruptField(views = @View(title = "折旧费用科目编码"),
                edit = @Edit(title = "折旧费用科目编码"))
    @Column(name = "dep_expense_account_code", length = 50)
    private String depreciationExpenseAccountCode;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = AstEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
