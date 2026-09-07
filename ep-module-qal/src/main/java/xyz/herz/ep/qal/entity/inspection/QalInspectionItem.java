package xyz.herz.ep.qal.entity.inspection;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.jpa.model.BaseModel;

import xyz.herz.ep.qal.entity.criteria.QalCriteria;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 质检读数明细(质检单子表,参考 ERPNext Quality Inspection 的 readings)。
 * <p>质检单提交时从启用的质检参数快照生成;录入读数后自动按规格上下限判定 result:
 * <ul>
 *   <li>上下限均空:目测类,由检查员勾选 result</li>
 *   <li>有限:lower &lt;= reading &lt;= upper 自动判合格,越界自动判不合格</li>
 * </ul>
 */
@Getter @Setter
@Entity
@Table(name = "qal_inspection_item")
public class QalInspectionItem extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    @EruptField(edit = @Edit(title = "质检单", show = false))
    private QalInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criteria_id", nullable = false)
    @EruptField(views = @View(title = "参数", column = "name"),
                edit = @Edit(title = "参数", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private QalCriteria criteria;

    @EruptField(views = @View(title = "参数名称"),
                edit = @Edit(title = "参数名称", show = false, desc = "提交时快照"))
    @Column(name = "criteria_name", length = 100)
    private String criteriaName;

    @EruptField(views = @View(title = "规格要求"),
                edit = @Edit(title = "规格要求", show = false, desc = "快照:如 [0,10.5] mm 或 目测"))
    @Column(name = "spec_text", length = 100)
    private String specText;

    @EruptField(views = @View(title = "读数值"),
                edit = @Edit(title = "读数值", desc = "录入后按规格自动判定"))
    @Column(name = "reading_value", precision = 18, scale = 4)
    private BigDecimal readingValue;

    @EruptField(views = @View(title = "合格"))
    @Column(name = "is_pass")
    private Boolean isPass;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 200)
    private String remark;
}
