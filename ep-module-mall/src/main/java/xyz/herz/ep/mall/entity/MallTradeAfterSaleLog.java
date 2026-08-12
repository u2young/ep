package xyz.herz.ep.mall.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 售后状态流转日志(每次状态变更追加一条)。 */
@Getter @Setter
@Entity
@Table(name = "mall_trade_after_sale_log")
@Erupt(name = "售后日志", power = @Power(add = false, edit = false, delete = false))
public class MallTradeAfterSaleLog extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "after_sale_id", nullable = false)
    @EruptField(views = @View(title = "售后单", column = "no"),
                edit = @Edit(title = "售后单", notNull = true))
    private MallTradeAfterSale afterSale;

    @EruptField(views = @View(title = "变更前状态"), edit = @Edit(title = "变更前状态", show = false))
    private Integer fromStatus;

    @EruptField(views = @View(title = "变更后状态"), edit = @Edit(title = "变更后状态", show = false))
    private Integer toStatus;

    @EruptField(views = @View(title = "操作人ID"), edit = @Edit(title = "操作人ID", show = false))
    private Long operatorId;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", show = false))
    @Column(length = 500)
    private String remark;
}
