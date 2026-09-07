package xyz.herz.ep.qal.entity.feedback;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 客户反馈(参考 ERPNext Issue/Feedback,轻量 CRUD 无状态机)。
 * <p>记录客户对产品/服务的质量反馈:评分 1-5,处理完成后回写 handled/handledAt/reply。
 */
@Getter @Setter
@Entity
@Table(name = "qal_feedback")
@Erupt(name = "客户反馈", power = @Power(importable = true, export = true))
public class QalFeedback extends MetaModelVo {

    @EruptField(views = @View(title = "反馈标题"),
                edit = @Edit(title = "反馈标题", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String title;

    @EruptField(views = @View(title = "客户名称"),
                edit = @Edit(title = "客户名称", notNull = true, search = @Search))
    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @EruptField(views = @View(title = "物料编码"),
                edit = @Edit(title = "物料编码"))
    @Column(name = "item_code", length = 50)
    private String itemCode;

    @EruptField(views = @View(title = "评分"),
                edit = @Edit(title = "评分(1-5)", notNull = true,
                    desc = "1 非常不满意 ~ 5 非常满意"))
    @Column(nullable = false)
    private Integer rating;

    @EruptField(views = @View(title = "反馈内容"),
                edit = @Edit(title = "反馈内容", notNull = true, type = EditType.TEXTAREA))
    @Column(length = 2000, nullable = false)
    private String content;

    @EruptField(views = @View(title = "是否已处理"),
                edit = @Edit(title = "是否已处理"))
    @Column(name = "is_handled")
    private Boolean isHandled = false;

    @EruptField(views = @View(title = "处理时间"),
                edit = @Edit(title = "处理时间", show = false, desc = "处理回写"))
    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @EruptField(views = @View(title = "回复说明"),
                edit = @Edit(title = "回复说明", type = EditType.TEXTAREA))
    @Column(length = 2000)
    private String reply;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
