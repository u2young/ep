package xyz.herz.ep.ast.entity.depreciation;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.ast.entity.asset.AstAsset;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 折旧计划表(参考 ERPNext Asset Depreciation Schedule DocType)。
 * <p>资产每期折旧一条记录,由 AstDepreciationCalculator 计算生成。
 * 字段:期次/折旧日期/折旧金额/累计折旧/净值/过账凭证ID。
 */
@Getter @Setter
@Entity
@Table(name = "ast_depreciation_schedule")
@Erupt(name = "折旧计划")
public class AstDepreciationSchedule extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @EruptField(views = @View(title = "资产", column = "asset_no"),
                edit = @Edit(title = "资产", type = EditType.REFERENCE_TABLE,
                    show = false))
    private AstAsset asset;

    @EruptField(views = @View(title = "期次"),
                edit = @Edit(title = "期次", notNull = true))
    @Column(name = "period_no", nullable = false)
    private Integer periodNo;

    @EruptField(views = @View(title = "折旧日期"),
                edit = @Edit(title = "折旧日期",
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "depreciation_date")
    private LocalDate depreciationDate;

    @EruptField(views = @View(title = "折旧金额"),
                edit = @Edit(title = "折旧金额", notNull = true))
    @Column(name = "depreciation_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal depreciationAmount;

    @EruptField(views = @View(title = "累计折旧"),
                edit = @Edit(title = "累计折旧"))
    @Column(name = "accumulated_depreciation", precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @EruptField(views = @View(title = "净值"),
                edit = @Edit(title = "净值"))
    @Column(name = "net_book_value", precision = 18, scale = 2)
    private BigDecimal netBookValue;

    @EruptField(views = @View(title = "已过账"),
                edit = @Edit(title = "已过账", show = false))
    @Column(name = "posted")
    private Boolean posted = false;

    @EruptField(views = @View(title = "凭证ID"),
                edit = @Edit(title = "凭证ID", show = false))
    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
