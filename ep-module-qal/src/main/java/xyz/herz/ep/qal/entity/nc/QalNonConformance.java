package xyz.herz.ep.qal.entity.nc;

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
import xyz.herz.ep.qal.core.QalEnumChoiceFetchHandler;
import xyz.herz.ep.qal.core.QalStateDataProxy;
import xyz.herz.ep.qal.enums.QalDictEnums.NcStatus;
import xyz.herz.ep.qal.handler.nc.QalNcHandler;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 不合格品处理单(参考 ERPNext Quality Non Conformance DocType)。
 * <p>状态机:0草稿/1已提交/2处理中/3已关闭/4已取消。
 * 来源:质检单判不合格自动生成,或手工创建;处置方式:退货/返工/让步接收/报废。
 */
@Getter @Setter
@Entity
@Table(name = "qal_non_conformance")
@Erupt(
    name = "不合格品处理",
    power = @Power(importable = true, export = true),
    dataProxy = QalNonConformance.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = QalNcHandler.CODE_SUBMIT,
            operationHandler = QalNcHandler.class),
        @RowOperation(title = "开始处理", code = QalNcHandler.CODE_PROCESS,
            operationHandler = QalNcHandler.class),
        @RowOperation(title = "关闭", code = QalNcHandler.CODE_CLOSE,
            operationHandler = QalNcHandler.class),
        @RowOperation(title = "取消", code = QalNcHandler.CODE_CANCEL,
            operationHandler = QalNcHandler.class)
    }
)
public class QalNonConformance extends MetaModelVo {

    @EruptField(views = @View(title = "N/C 单号"),
                edit = @Edit(title = "N/C 单号", notNull = true, search = @Search))
    @Column(name = "nc_no", length = 50, nullable = false)
    private String ncNo;

    @EruptField(views = @View(title = "来源质检单"),
                edit = @Edit(title = "来源质检单", search = @Search))
    @Column(name = "source_inspection_no", length = 50)
    private String sourceInspectionNo;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码", notNull = true, search = @Search))
    @Column(name = "item_code", length = 50, nullable = false)
    private String itemCode;

    @EruptField(views = @View(title = "物料名称"),
                edit = @Edit(title = "物料名称", notNull = true))
    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @EruptField(views = @View(title = "批次号"),
                edit = @Edit(title = "批次号"))
    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @EruptField(views = @View(title = "不合格数量"),
                edit = @Edit(title = "不合格数量", notNull = true))
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal qty;

    @EruptField(views = @View(title = "问题描述"),
                edit = @Edit(title = "问题描述", notNull = true, type = EditType.TEXTAREA))
    @Column(name = "description", length = 1000, nullable = false)
    private String description;

    @EruptField(views = @View(title = "处置方式"),
                edit = @Edit(title = "处置方式",
                    choiceType = @ChoiceType(fetchHandler = QalEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "NcDisposition")))
    @Column(name = "disposition")
    private Integer disposition;

    @EruptField(views = @View(title = "责任人"),
                edit = @Edit(title = "责任人"))
    @Column(name = "handler_name", length = 50)
    private String handlerName;

    @EruptField(views = @View(title = "处理说明"),
                edit = @Edit(title = "处理说明", desc = "关闭时必填"))
    @Column(name = "process_note", length = 1000)
    private String processNote;

    @EruptField(views = @View(title = "关闭日期"),
                edit = @Edit(title = "关闭日期", show = false, desc = "关闭时回写"))
    @Column(name = "closed_date")
    private LocalDate closedDate;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = QalEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "NcStatus")))
    @Column(nullable = false)
    private Integer status = NcStatus.DRAFT.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends QalStateDataProxy<QalNonConformance> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
