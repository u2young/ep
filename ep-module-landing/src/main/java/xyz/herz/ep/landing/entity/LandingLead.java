package xyz.herz.ep.landing.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.landing.core.LandingEnumChoiceFetchHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 留资记录。
 * <p>由 H5 落地页表单提交(magic-api /landing/lead 接口)写入。
 * 扩展字段 extra 以 JSON 存储表单任意字段(name/age/邮箱等)。
 */
@Getter @Setter
@Entity
@Table(name = "landing_lead")
@Erupt(name = "留资记录", power = @Power(importable = true, export = true))
public class LandingLead extends BaseModel {

    @EruptField(views = @View(title = "落地页ID"),
                edit = @Edit(title = "落地页ID", search = @Search))
    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @EruptField(views = @View(title = "Slug"),
                edit = @Edit(title = "Slug", show = false))
    @Column(length = 64)
    private String slug;

    @EruptField(views = @View(title = "手机号"),
                edit = @Edit(title = "手机号", search = @Search))
    @Column(length = 20)
    private String phone;

    /** 表单其它字段以 JSON 存,name/age/邮箱等任意扩展 */
    @EruptField(views = @View(title = "扩展字段"),
                edit = @Edit(title = "扩展字段(JSON)", type = EditType.TEXTAREA, show = false))
    @Lob @Column(columnDefinition = "TEXT")
    private String extra;

    @EruptField(views = @View(title = "来源"),
                edit = @Edit(title = "来源", search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "LeadSource")))
    @Column
    private Integer source;

    @EruptField(views = @View(title = "提交IP"),
                edit = @Edit(title = "提交IP", show = false))
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @EruptField(views = @View(title = "User-Agent"),
                edit = @Edit(title = "User-Agent", type = EditType.TEXTAREA, show = false))
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @EruptField(views = @View(title = "提交时间"),
                edit = @Edit(title = "提交时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "submit_time")
    private LocalDateTime submitTime;
}
