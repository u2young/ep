package xyz.herz.ep.pur.entity.requisition;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pur.core.PurEnumChoiceFetchHandler;
import xyz.herz.ep.pur.core.PurStateDataProxy;
import xyz.herz.ep.pur.enums.PurDictEnums.Priority;
import xyz.herz.ep.pur.enums.PurDictEnums.RequisitionStatus;
import xyz.herz.ep.pur.handler.requisition.PurRequisitionLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 请购单(参考 ERPNext Material Request DocType)。
 * <p>状态机:0草稿/1已提交/2已转单/3已取消。
 * 提交后可被采购员拿来创建询价单或采购订单;转单后置为「已转单」终态。
 */
@Getter @Setter
@Entity
@Table(name = "pur_requisition")
@Erupt(
    name = "请购单",
    power = @Power(importable = true, export = true),
    dataProxy = PurPurchaseRequisition.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = PurRequisitionLifecycleHandler.CODE_SUBMIT,
            operationHandler = PurRequisitionLifecycleHandler.class),
        @RowOperation(title = "转单", code = PurRequisitionLifecycleHandler.CODE_CONVERT,
            operationHandler = PurRequisitionLifecycleHandler.class),
        @RowOperation(title = "取消", code = PurRequisitionLifecycleHandler.CODE_CANCEL,
            operationHandler = PurRequisitionLifecycleHandler.class)
    }
)
public class PurPurchaseRequisition extends MetaModelVo {

    @EruptField(views = @View(title = "请购单号"),
                edit = @Edit(title = "请购单号", notNull = true, search = @Search))
    @Column(name = "requisition_no", length = 50, nullable = false)
    private String requisitionNo;

    @EruptField(views = @View(title = "请购人"),
                edit = @Edit(title = "请购人", notNull = true))
    @Column(name = "requester", length = 50, nullable = false)
    private String requester;

    @EruptField(views = @View(title = "请购部门"),
                edit = @Edit(title = "请购部门"))
    @Column(name = "department", length = 50)
    private String department;

    @EruptField(views = @View(title = "需求日期"),
                edit = @Edit(title = "需求日期", notNull = true))
    @Column(name = "required_date", nullable = false)
    private LocalDate requiredDate;

    @EruptField(views = @View(title = "优先级"),
                edit = @Edit(title = "优先级", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PurEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "Priority")))
    @Column(nullable = false)
    private Integer priority = Priority.MEDIUM.code;

    @EruptField(views = @View(title = "预估总额"),
                edit = @Edit(title = "预估总额", desc = "明细合计"))
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PurEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "RequisitionStatus")))
    @Column(nullable = false)
    private Integer status = RequisitionStatus.DRAFT.code;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "请购明细", type = EditType.TAB_TABLE_ADD))
    private List<PurRequisitionItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends PurStateDataProxy<PurPurchaseRequisition> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
