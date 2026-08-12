package xyz.herz.ep.crm.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
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
 * 回款记录(P1 扩展)。
 *
 * <p>每次实际收到一笔款就建一条记录,通过 {@link xyz.herz.ep.crm.handler.CrmReceivableConfirmHandler}
 * 把金额累加到关联的回款计划 {@link CrmReceivablePlan#getReceivedAmount()},
 * 并刷新计划状态(部分回款 / 已回款)。
 *
 * <p>{@code plan} 可空:不挂计划时,合同回款总额不能由计划汇总得到,需要单独统计。
 */
@Getter
@Setter
@Table(name = "crm_receivable_record")
@Entity
@Erupt(name = "回款记录", power = @Power(importable = true, export = true))
public class CrmReceivableRecord extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "contract_id")
    @EruptField(
        views = @View(title = "合同", column = "no"),
        edit = @Edit(title = "合同", notNull = true, search = @Search)
    )
    private CrmContract contract;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    @EruptField(
        views = @View(title = "回款计划", column = "id"),
        edit = @Edit(title = "回款计划", desc = "可空;选择后由【确认回款】按钮把金额累加到该计划")
    )
    private CrmReceivablePlan plan;

    @EruptField(
        views = @View(title = "实际回款金额", sortable = true),
        edit = @Edit(title = "实际回款金额", notNull = true, type = EditType.NUMBER)
    )
    private BigDecimal amount;

    @EruptField(
        views = @View(title = "实际回款日期", sortable = true),
        edit = @Edit(title = "实际回款日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE))
    )
    private LocalDate receivedDate;

    @Lob
    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;
}
