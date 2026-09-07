package xyz.herz.ep.proj.entity.cashflow;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.proj.core.ProjEnumChoiceFetchHandler;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.proj.enums.ProjDictEnums.CashFlowType;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 项目现金流(参考 ERPNext Project Cash Flow)。
 * <p>flowType: INFLOW(1流入)/OUTFLOW(2流出);每行一笔资金进出。
 * <p>sourceType/sourceId/sourceNo 记录来源单据(费用报销/销售收款/采购付款等),支持回溯。
 * 由各模块 Handler 在入账/收付款时插入(本模块 ProjExpenseClaim.post 亦插入一笔 OUTFLOW)。
 */
@Getter @Setter
@Entity
@Table(name = "proj_cash_flow")
@Erupt(name = "项目现金流", power = @Power(importable = true, export = true))
public class ProjCashFlow extends MetaModelVo {

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "project_id")
    @EruptField(views = @View(title = "项目", column = "name"),
                edit = @Edit(title = "项目", notNull = true, type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private ProjProject project;

    @EruptField(views = @View(title = "流向"),
                edit = @Edit(title = "流向", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = ProjEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "CashFlowType")))
    @Column(name = "flow_type", nullable = false)
    private Integer flowType = CashFlowType.OUTFLOW.code;

    @EruptField(views = @View(title = "金额"),
                edit = @Edit(title = "金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "amount", precision = 24, scale = 6, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "入账日期"),
                edit = @Edit(title = "入账日期", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE)))
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @EruptField(views = @View(title = "来源类型"),
                edit = @Edit(title = "来源类型"))
    @Column(name = "source_type", length = 60)
    private String sourceType;

    @EruptField(views = @View(title = "来源ID"), edit = @Edit(title = "来源ID", show = false))
    @Column(name = "source_id")
    private Long sourceId;

    @EruptField(views = @View(title = "来源单号"))
    @Column(name = "source_no", length = 60)
    private String sourceNo;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 1000)
    private String remark;
}
