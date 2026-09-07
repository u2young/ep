package xyz.herz.ep.stk.entity.batch;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.stk.core.StkEnumChoiceFetchHandler;
import xyz.herz.ep.stk.enums.StkDictEnums.EnableStatus;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批次(参考 ERPNext Batch DocType,主数据)。
 * <p>无状态机,CRUD + 启停;追踪物料批次,含制造日期/有效期/初始数量。
 */
@Getter @Setter
@Entity
@Table(name = "stk_batch")
@Erupt(name = "批次", power = @Power(importable = true, export = true))
public class StkBatch extends MetaModelVo {

    @EruptField(views = @View(title = "批次号"),
                edit = @Edit(title = "批次号", notNull = true, search = @Search))
    @Column(name = "batch_no", length = 50, nullable = false)
    private String batchNo;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码", notNull = true, search = @Search))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"))
    @Column(name = "item_name", length = 100)
    private String itemName;

    @EruptField(views = @View(title = "制造日期"),
                edit = @Edit(title = "制造日期"))
    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @EruptField(views = @View(title = "有效期至"),
                edit = @Edit(title = "有效期至", notNull = true))
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @EruptField(views = @View(title = "初始数量"),
                edit = @Edit(title = "初始数量", notNull = true))
    @Column(name = "initial_qty", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialQty;

    @EruptField(views = @View(title = "供应商编码"))
    @Column(name = "supplier_code", length = 50)
    private String supplierCode;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = StkEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
