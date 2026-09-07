package xyz.herz.ep.stk.entity.serial;

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
 * 序列号(参考 ERPNext Serial No DocType,主数据)。
 * <p>无状态机,CRUD + 启停;追踪单件物料的序列号,可关联批次/仓库/入库成本。
 */
@Getter @Setter
@Entity
@Table(name = "stk_serial_no")
@Erupt(name = "序列号", power = @Power(importable = true, export = true))
public class StkSerialNo extends MetaModelVo {

    @EruptField(views = @View(title = "序列号"),
                edit = @Edit(title = "序列号", notNull = true, search = @Search))
    @Column(name = "serial_no", length = 100, nullable = false)
    private String serialNo;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码", notNull = true, search = @Search))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"))
    @Column(name = "item_name", length = 100)
    private String itemName;

    @EruptField(views = @View(title = "批次号"),
                edit = @Edit(title = "批次号", search = @Search))
    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @EruptField(views = @View(title = "仓库编码"))
    @Column(name = "warehouse_code", length = 50)
    private String warehouseCode;

    @EruptField(views = @View(title = "仓库名称"))
    @Column(name = "warehouse_name", length = 100)
    private String warehouseName;

    @EruptField(views = @View(title = "入库日期"),
                edit = @Edit(title = "入库日期"))
    @Column(name = "warranty_date")
    private LocalDate warrantyDate;

    @EruptField(views = @View(title = "入库成本"))
    @Column(name = "purchase_cost", precision = 18, scale = 2)
    private BigDecimal purchaseCost;

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
