package xyz.herz.ep.erp.entity.finance;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.VL;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 核销记录(付款单/收款单 ↔ 采购订单/销售订单 多对多)。
 * <p>P1 简化:用独立表 + 单号快照,不直接外键关联(避免单据多类型耦合)。
 * <ul>
 *   <li>bizType: 1=付款单 2=收款单(指本表记录的「收/付方」)</li>
 *   <li>targetBizType: 1=采购订单 2=销售订单 3=其他(指被核销的目标单据)</li>
 *   <li>docId / docNo: 收/付款单的 ID 和单号快照</li>
 *   <li>targetId / targetNo: 目标单据的 ID 和单号快照</li>
 *   <li>amount: 本次核销金额</li>
 * </ul>
 */
@Getter @Setter
@Entity
@Table(name = "erp_finance_settlement",
    indexes = {
        @Index(name = "idx_fin_settle_doc", columnList = "biz_type, doc_id"),
        @Index(name = "idx_fin_settle_target", columnList = "target_biz_type, target_id")
    })
@Erupt(name = "核销记录")
public class ErpFinanceSettlement extends BaseModel {

    @EruptField(views = @View(title = "业务类型"),
                edit = @Edit(title = "业务类型", notNull = true, type = EditType.CHOICE,
                    choiceType = @ChoiceType(vl = {
                        @VL(value = "1", label = "付款"),
                        @VL(value = "2", label = "收款")
                    })))
    @Column(name = "biz_type", nullable = false)
    private Integer bizType;

    @EruptField(views = @View(title = "单据ID"),
                edit = @Edit(title = "单据ID", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @EruptField(views = @View(title = "单号"),
                edit = @Edit(title = "单号", notNull = true))
    @Column(name = "doc_no", length = 60, nullable = false)
    private String docNo;

    @EruptField(views = @View(title = "目标业务类型"),
                edit = @Edit(title = "目标业务类型", notNull = true, type = EditType.CHOICE,
                    choiceType = @ChoiceType(vl = {
                        @VL(value = "1", label = "采购订单"),
                        @VL(value = "2", label = "销售订单"),
                        @VL(value = "3", label = "其他")
                    })))
    @Column(name = "target_biz_type", nullable = false)
    private Integer targetBizType;

    @EruptField(views = @View(title = "目标单据ID"),
                edit = @Edit(title = "目标单据ID", notNull = true, numberType = @NumberType(min = 0)))
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @EruptField(views = @View(title = "目标单号"),
                edit = @Edit(title = "目标单号", notNull = true))
    @Column(name = "target_no", length = 60, nullable = false)
    private String targetNo;

    @EruptField(views = @View(title = "核销金额"),
                edit = @Edit(title = "核销金额", notNull = true, numberType = @NumberType(min = 0)))
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal amount = BigDecimal.ZERO;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
