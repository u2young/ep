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
import xyz.herz.ep.landing.enums.LandingDictEnums.CouponStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.CouponType;
import xyz.herz.ep.landing.handler.CouponEnableHandler;
import xyz.herz.ep.landing.handler.CouponDisableHandler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 优惠券模板。
 * <p>定义优惠券的面值/折扣、使用门槛、有效期、可领取总量等规则。
 * 启用后可在 H5 页面通过 couponCode 领取。
 */
@Getter @Setter
@Entity
@Table(name = "landing_coupon")
@Erupt(
    name = "优惠券模板",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "启用", code = CouponEnableHandler.CODE, icon = "fa fa-check",
            operationHandler = CouponEnableHandler.class, operationParam = { CouponEnableHandler.CODE }),
        @RowOperation(title = "禁用", code = CouponDisableHandler.CODE, icon = "fa fa-ban",
            operationHandler = CouponDisableHandler.class, operationParam = { CouponDisableHandler.CODE })
    }
)
public class LandingCoupon extends BaseModel {

    @EruptField(views = @View(title = "券名称"),
                edit = @Edit(title = "券名称", notNull = true, search = @Search,
                    desc = "用户可见的优惠文案,如「满100减20」"))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "优惠码"),
                edit = @Edit(title = "优惠码", notNull = true, search = @Search,
                    desc = "用户输入的领取码,全大写6位字母数字组合,用于H5填写领取"))
    @Column(name = "coupon_code", length = 20, nullable = false, updatable = false)
    private String couponCode;

    @EruptField(views = @View(title = "类型"),
                edit = @Edit(title = "类型", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "CouponType")))
    @Column(nullable = false)
    private Integer type;

    /** 面值(满减)或折扣率(折扣券)。满减券:优惠金额(元);折扣券:折扣系数,0.8表示8折 */
    @EruptField(views = @View(title = "面值/折扣率"),
                edit = @Edit(title = "面值/折扣率", notNull = true, search = @Search,
                    desc = "满减券填优惠金额(元);折扣券填折扣系数(如0.8=8折)"))
    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal value;

    @EruptField(views = @View(title = "使用门槛(元)"),
                edit = @Edit(title = "使用门槛(元)",
                    desc = "订单金额达到此值方可使用,0 表示无门槛"))
    @Column(name = "min_amount", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal minAmount = java.math.BigDecimal.ZERO;

    @EruptField(views = @View(title = "每人限领"),
                edit = @Edit(title = "每人限领数量",
                    desc = "限制同一手机号最多领取几张,默认1"))
    @Column(name = "limit_per_user", nullable = false)
    private Integer limitPerUser = 1;

    @EruptField(views = @View(title = "可领取总量"),
                edit = @Edit(title = "可领取总量", search = @Search,
                    desc = "总发放上限,0 表示不限"))
    @Column(name = "total_limit")
    private Integer totalLimit = 0;

    @EruptField(views = @View(title = "已领取"),
                edit = @Edit(title = "已领取", show = false))
    @Column(name = "claimed_count", nullable = false)
    private Integer claimedCount = 0;

    @EruptField(views = @View(title = "开始领取"),
                edit = @Edit(title = "开始领取",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @EruptField(views = @View(title = "结束领取"),
                edit = @Edit(title = "结束领取",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @EruptField(views = @View(title = "有效期(天)"),
                edit = @Edit(title = "领取后有效期(天)",
                    desc = "领取后多少天内有效,0 表示永久"))
    @Column(name = "valid_days")
    private Integer validDays = 0;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "CouponStatus")))
    @Column(nullable = false)
    private Integer status = CouponStatus.DRAFT.code;

    @EruptField(views = @View(title = "说明"),
                edit = @Edit(title = "说明", type = EditType.TEXTAREA, show = false))
    @Column(columnDefinition = "TEXT")
    private String remark;
}
