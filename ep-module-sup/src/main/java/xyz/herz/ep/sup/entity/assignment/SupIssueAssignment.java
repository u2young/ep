package xyz.herz.ep.sup.entity.assignment;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.sup.entity.issue.SupIssue;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 工单分派历史明细(子表,参考 ERPNext Issue Assignment)。
 * <p>每次分派/转派生成一条历史记录;SupIssue.assignments 级联管理。
 */
@Getter @Setter
@Entity
@Table(name = "sup_issue_assignment")
@Erupt(name = "工单分派历史")
public class SupIssueAssignment extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @EruptField(views = @View(title = "工单", column = "subject"),
                edit = @Edit(title = "工单", notNull = true,
                    type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "subject")))
    private SupIssue issue;

    @EruptField(views = @View(title = "分派人ID"), edit = @Edit(title = "分派人ID", show = false))
    @Column(name = "assignee_id")
    private Long assigneeId;

    @EruptField(views = @View(title = "分派人"), edit = @Edit(title = "分派人"))
    @Column(length = 100)
    private String assigneeName;

    @EruptField(views = @View(title = "分派时间"), edit = @Edit(title = "分派时间", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String note;
}
