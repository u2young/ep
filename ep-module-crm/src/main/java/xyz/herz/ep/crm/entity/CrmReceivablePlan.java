package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/**
 * 回款计划(P1 扩展)。
 *
 * <p>一张合同可有多期回款计划,按 {@code periodNo} 排序。状态机:
 * <ul>
 *   <li>0 待回款(PENDING)</li>
 *   <li>1 部分回款(PARTIAL)— 已回款金额 &gt; 0 但 &lt; planAmount</li>
 *   <li>2 已回款(RECEIVED)— 已回款金额 ≥ planAmount</li>
 * </ul>
 *
 * <p>{@code receivedAmount} 由 {@link xyz.herz.ep.crm.handler.CrmReceivableConfirmHandler}
 * 在确认回款记录时累加并刷新 status,不要在表单直接改。
 */
@Getter
@Setter
@Table(name = "crm_receivable_plan")
@Entity
@Erupt(name = "回款计划", power = @Power(importable = true, export = true))
public class CrmReceivablePlan extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @EruptField(
        views = @View(title = "合同", column = "no"),
        edit = @Edit(title = "合同", notNull = true, search = @Search)
    )
    private CrmContract contract;

    @EruptField(
        views = @View(title = "期次", sortable = true),
        edit = @Edit(title = "期次", notNull = true, type = EditType.NUMBER, desc = "如 1/2/3 表示第几期")
    )
    private Integer periodNo;

    @EruptField(
        views = @View(title = "计划回款金额", sortable = true),
        edit = @Edit(title = "计划回款金额", notNull = true, type = EditType.NUMBER)
    )
    private BigDecimal planAmount;

    @EruptField(
        views = @View(title = "计划回款日期", sortable = true),
        edit = @Edit(title = "计划回款日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE))
    )
    private LocalDate planDate;

    @EruptField(
        views = @View(title = "已回款金额", sortable = true),
        edit = @Edit(title = "已回款金额", type = EditType.NUMBER, show = false,
            desc = "由【确认回款】按钮累加,不要直接修改")
    )
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @EruptField(
        views = @View(title = "状态", sortable = true),
        edit = @Edit(
            title = "状态", type = EditType.CHOICE,
            desc = "由【确认回款】按钮根据已回款金额自动刷新,不要直接修改",
            choiceType = @ChoiceType(
                fetchHandler = CrmEnumChoiceFetchHandler.class,
                fetchHandlerParams = {"ReceivableStatus"}
            )
        )
    )
    private Integer status = 0;

    @Lob
    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;
}
