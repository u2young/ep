package xyz.herz.ep.pur.entity.rfq;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pur.core.PurEnumChoiceFetchHandler;
import xyz.herz.ep.pur.core.PurStateDataProxy;
import xyz.herz.ep.pur.enums.PurDictEnums.RfqStatus;
import xyz.herz.ep.pur.handler.rfq.PurRfqLifecycleHandler;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 询价单(参考 ERPNext Request for Quotation DocType)。
 * <p>状态机:0草稿/1已发送/2已收报价/3已取消。
 * 向多个供应商发出询价,收到回复后置「已收报价」,然后可挑选采纳哪个供应商报价。
 */
@Getter @Setter
@Entity
@Table(name = "pur_rfq")
@Erupt(
    name = "询价单",
    power = @Power(importable = true, export = true),
    dataProxy = PurRequestForQuotation.Proxy.class,
    rowOperation = {
        @RowOperation(title = "发送", code = PurRfqLifecycleHandler.CODE_SEND,
            operationHandler = PurRfqLifecycleHandler.class),
        @RowOperation(title = "收报价", code = PurRfqLifecycleHandler.CODE_RECEIVE,
            operationHandler = PurRfqLifecycleHandler.class),
        @RowOperation(title = "取消", code = PurRfqLifecycleHandler.CODE_CANCEL,
            operationHandler = PurRfqLifecycleHandler.class)
    }
)
public class PurRequestForQuotation extends MetaModelVo {

    @EruptField(views = @View(title = "询价单号"),
                edit = @Edit(title = "询价单号", notNull = true, search = @Search))
    @Column(name = "rfq_no", length = 50, nullable = false)
    private String rfqNo;

    @EruptField(views = @View(title = "来源请购单"),
                edit = @Edit(title = "来源请购单", search = @Search, desc = "可空:手工创建"))
    @Column(name = "source_requisition_no", length = 50)
    private String sourceRequisitionNo;

    @EruptField(views = @View(title = "采购员"),
                edit = @Edit(title = "采购员"))
    @Column(name = "buyer", length = 50)
    private String buyer;

    @EruptField(views = @View(title = "截止日期"),
                edit = @Edit(title = "截止日期", notNull = true, desc = "供应商回复截止"))
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @EruptField(views = @View(title = "收报价时间"),
                edit = @Edit(title = "收报价时间", show = false, desc = "标记收报价时回写"))
    @Column(name = "received_at")
    private java.time.LocalDateTime receivedAt;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PurEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "RfqStatus")))
    @Column(nullable = false)
    private Integer status = RfqStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends PurStateDataProxy<PurRequestForQuotation> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
