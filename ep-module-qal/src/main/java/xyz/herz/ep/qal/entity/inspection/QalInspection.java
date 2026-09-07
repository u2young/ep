package xyz.herz.ep.qal.entity.inspection;

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
import xyz.herz.ep.qal.enums.QalDictEnums.InspectionStatus;
import xyz.herz.ep.qal.handler.inspection.QalInspectionLifecycleHandler;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 质检单(参考 ERPNext Quality Inspection DocType)。
 * <p>状态机:0草稿/1待检/2合格/3不合格/4已取消。
 * 提交时按启用的质检参数快照生成读数明细;全部读数合格可判「合格」,存在不合格读数判「不合格」;
 * 判不合格时自动生成不合格品处理单(N/C)。
 */
@Getter @Setter
@Entity
@Table(name = "qal_inspection")
@Erupt(
    name = "质检单",
    power = @Power(importable = true, export = true),
    dataProxy = QalInspection.Proxy.class,
    rowOperation = {
        @RowOperation(title = "提交", code = QalInspectionLifecycleHandler.CODE_SUBMIT,
            operationHandler = QalInspectionLifecycleHandler.class),
        @RowOperation(title = "判定合格", code = QalInspectionLifecycleHandler.CODE_PASS,
            operationHandler = QalInspectionLifecycleHandler.class),
        @RowOperation(title = "判定不合格", code = QalInspectionLifecycleHandler.CODE_FAIL,
            operationHandler = QalInspectionLifecycleHandler.class),
        @RowOperation(title = "取消", code = QalInspectionLifecycleHandler.CODE_CANCEL,
            operationHandler = QalInspectionLifecycleHandler.class)
    }
)
public class QalInspection extends MetaModelVo {

    @EruptField(views = @View(title = "质检单号"),
                edit = @Edit(title = "质检单号", notNull = true, search = @Search))
    @Column(name = "inspection_no", length = 50, nullable = false)
    private String inspectionNo;

    @EruptField(views = @View(title = "来源类型"),
                edit = @Edit(title = "来源类型", desc = "如 PurchaseReceipt/WorkOrder,自定义快照"))
    @Column(name = "source_type", length = 50)
    private String sourceType;

    @EruptField(views = @View(title = "来源单号"),
                edit = @Edit(title = "来源单号", search = @Search))
    @Column(name = "source_no", length = 50)
    private String sourceNo;

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

    @EruptField(views = @View(title = "质检数量"),
                edit = @Edit(title = "质检数量", notNull = true))
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal qty;

    @EruptField(views = @View(title = "抽检数量"),
                edit = @Edit(title = "抽检数量"))
    @Column(name = "sample_qty", precision = 18, scale = 2)
    private BigDecimal sampleQty;

    @EruptField(views = @View(title = "检查员"),
                edit = @Edit(title = "检查员"))
    @Column(name = "inspector", length = 50)
    private String inspector;

    @EruptField(views = @View(title = "检查日期"),
                edit = @Edit(title = "检查日期"))
    @Column(name = "inspected_date")
    private LocalDate inspectedDate;

    @EruptField(views = @View(title = "判定时间"),
                edit = @Edit(title = "判定时间", show = false, desc = "判定合格/不合格时回写"))
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = QalEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "InspectionStatus")))
    @Column(nullable = false)
    private Integer status = InspectionStatus.DRAFT.code;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, fetch = FetchType.EAGER,
               orphanRemoval = true)
    @EruptField(edit = @Edit(title = "读数明细", type = EditType.TAB_TABLE_ADD))
    private List<QalInspectionItem> items = new ArrayList<>();

    @EruptField(views = @View(title = "不合格单号"),
                edit = @Edit(title = "不合格单号", show = false, desc = "判不合格后自动生成 N/C 单号回写"))
    @Column(name = "nc_no", length = 50)
    private String ncNo;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends QalStateDataProxy<QalInspection> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
