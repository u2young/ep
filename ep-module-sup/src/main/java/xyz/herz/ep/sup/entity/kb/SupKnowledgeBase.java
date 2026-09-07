package xyz.herz.ep.sup.entity.kb;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.*;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.sup.core.SupEnumChoiceFetchHandler;
import xyz.herz.ep.sup.core.SupStateDataProxy;
import xyz.herz.ep.sup.enums.SupDictEnums.KbStatus;
import xyz.herz.ep.sup.handler.kb.SupKbPublishHandler;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识库文章(参考 ERPNext Knowledge Base)。
 * <p>状态机:0草稿 → 1已发布 → 2已归档;通过行按钮发布/归档切换。
 * viewCount 为浏览计数(派生,默认 0);publishedAt 在发布时回写。
 */
@Getter @Setter
@Entity
@Table(name = "sup_kb_article")
@Erupt(
    name = "知识库",
    power = @Power(importable = true, export = true),
    dataProxy = SupKnowledgeBase.Proxy.class,
    rowOperation = {
        @RowOperation(title = "发布", code = SupKbPublishHandler.CODE_PUBLISH,
            operationHandler = SupKbPublishHandler.class,
            operationParam = { SupKbPublishHandler.CODE_PUBLISH }),
        @RowOperation(title = "归档", code = SupKbPublishHandler.CODE_ARCHIVE,
            operationHandler = SupKbPublishHandler.class,
            operationParam = { SupKbPublishHandler.CODE_ARCHIVE })
    }
)
public class SupKnowledgeBase extends MetaModelVo {

    @EruptField(views = @View(title = "标题"),
                edit = @Edit(title = "标题", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String title;

    @EruptField(views = @View(title = "分类"), edit = @Edit(title = "分类"))
    @Column(length = 60)
    private String category;

    @EruptField(views = @View(title = "正文"), edit = @Edit(title = "正文", type = EditType.TEXTAREA))
    @Lob
    @Column(name = "content", columnDefinition = "CLOB")
    private String content;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = SupEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "KbStatus")))
    @Column(nullable = false)
    private Integer status = KbStatus.DRAFT.code;

    @EruptField(views = @View(title = "浏览数"), edit = @Edit(title = "浏览数", show = false))
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @EruptField(views = @View(title = "作者ID"), edit = @Edit(title = "作者ID", show = false))
    @Column(name = "author_id")
    private Long authorId;

    @EruptField(views = @View(title = "作者"), edit = @Edit(title = "作者"))
    @Column(length = 100)
    private String authorName;

    @EruptField(views = @View(title = "发布时间"), edit = @Edit(title = "发布时间", show = false,
        dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends SupStateDataProxy<SupKnowledgeBase> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
