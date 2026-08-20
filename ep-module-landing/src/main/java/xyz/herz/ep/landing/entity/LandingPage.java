package xyz.herz.ep.landing.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.landing.core.LandingEnumChoiceFetchHandler;
import xyz.herz.ep.landing.core.LandingStateDataProxy;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.handler.LandingOfflineHandler;
import xyz.herz.ep.landing.handler.LandingPublishHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 落地页(主表)。
 * <p>状态机:0 草稿 → 10 已发布 → 20 已下线;下线可重新发布回 10。
 * 状态字段锁定,只能通过行按钮(发布/下线)变更。
 * <p>短码在发布时生成(Base62(id + 1_000_000)),下线时保留但访问返回 410 Gone。
 */
@Getter @Setter
@Entity
@Table(name = "landing_page",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_landing_slug",  columnNames = "slug"),
           @UniqueConstraint(name = "uk_landing_short", columnNames = "short_code")
       })
@Erupt(
    name = "落地页",
    power = @Power(importable = true, export = true),
    dataProxy = LandingPage.Proxy.class,
    rowOperation = {
        @RowOperation(title = "发布", code = LandingPublishHandler.CODE, icon = "fa fa-share-square",
            operationHandler = LandingPublishHandler.class, operationParam = { LandingPublishHandler.CODE }),
        @RowOperation(title = "下线", code = LandingOfflineHandler.CODE, icon = "fa fa-power-off",
            operationHandler = LandingOfflineHandler.class, operationParam = { LandingOfflineHandler.CODE })
    }
)
public class LandingPage extends BaseModel {

    /** 业务 slug:URL 直链 /p/{slug} 的标识,创建时填写,不可改 */
    @EruptField(views = @View(title = "Slug"),
                edit = @Edit(title = "Slug", notNull = true, search = @Search,
                    desc = "用于直链访问 /p/{slug},创建后不可修改"))
    @Column(length = 64, nullable = false, updatable = false)
    private String slug;

    @EruptField(views = @View(title = "页面名称"),
                edit = @Edit(title = "页面名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "PageStatus")))
    @Column(nullable = false)
    private Integer status = PageStatus.DRAFT.code;

    /** amis schema JSON,TEXT 类型可存大文本;erupt 用 TEXTAREA 编辑原 JSON */
    @EruptField(views = @View(title = "页面内容"),
                edit = @Edit(title = "页面内容(amis JSON)", type = EditType.TEXTAREA))
    @Lob @Column(columnDefinition = "TEXT")
    private String content;

    /** 模板ID(可选):从模板创建时回填,便于追溯 */
    @EruptField(views = @View(title = "模板ID"),
                edit = @Edit(title = "模板ID", search = @Search))
    @Column(name = "template_id")
    private Long templateId;

    /** 短码:发布时生成,下线时保留但访问跳 410 Gone */
    @EruptField(views = @View(title = "短码"),
                edit = @Edit(title = "短码", show = false))
    @Column(name = "short_code", length = 16, updatable = false)
    private String shortCode;

    @EruptField(views = @View(title = "发布时间"),
                edit = @Edit(title = "发布时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @EruptField(views = @View(title = "下线时间"),
                edit = @Edit(title = "下线时间", show = false,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "offline_time")
    private LocalDateTime offlineTime;

    /** PV/UV 冗余计数,访问日志聚合回写;Erupt 列表只读展示 */
    @EruptField(views = @View(title = "PV"),
                edit = @Edit(title = "PV", show = false))
    @Column(name = "pv_count")
    private Long pvCount = 0L;

    @EruptField(views = @View(title = "UV"),
                edit = @Edit(title = "UV", show = false))
    @Column(name = "uv_count")
    private Long uvCount = 0L;

    /** 状态机锁:仅允许草稿态(0/null)直接编辑 status,其它通过行按钮 */
    public static class Proxy extends LandingStateDataProxy<LandingPage> {
        @Override protected String stateFieldName() { return "status"; }
        @Override protected Object[] allowedDirectEditStatuses() {
            return new Object[]{ PageStatus.DRAFT.code, null };
        }
    }
}
