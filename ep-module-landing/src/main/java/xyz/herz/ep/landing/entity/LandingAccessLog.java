package xyz.herz.ep.landing.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 落地页访问日志(PV/UV 去重)。
 * <p>(page_id, client_ip, client_fp) 唯一约束 → 同 IP+UA 当日不重复计 UV。
 * client_fp = MD5(clientIp + userAgent) 取前 32 位。
 */
@Getter @Setter
@Entity
@Table(name = "landing_access_log",
       uniqueConstraints = @UniqueConstraint(name = "uk_landing_uv",
           columnNames = {"page_id", "client_ip", "client_fp"}))
@Erupt(name = "访问日志", power = @Power(importable = false, export = true))
public class LandingAccessLog extends BaseModel {

    @EruptField(views = @View(title = "落地页ID"), edit = @Edit(title = "落地页ID"))
    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @EruptField(views = @View(title = "Slug"), edit = @Edit(title = "Slug"))
    @Column(length = 64)
    private String slug;

    @EruptField(views = @View(title = "客户端IP"), edit = @Edit(title = "客户端IP"))
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /** 指纹:IP + UA hash(MD5),用于 UV 去重 */
    @EruptField(views = @View(title = "指纹"), edit = @Edit(title = "指纹"))
    @Column(name = "client_fp", length = 64)
    private String clientFp;

    @EruptField(views = @View(title = "访问时间"),
                edit = @Edit(title = "访问时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "access_time")
    private LocalDateTime accessTime;
}
