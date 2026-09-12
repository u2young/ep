package xyz.herz.ep.landing.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;
import xyz.herz.ep.landing.core.LandingEnumChoiceFetchHandler;
import xyz.herz.ep.landing.enums.LandingDictEnums.UserCouponStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户优惠券领取记录。
 * <p>用户通过优惠券码领取后写入,初始 UNUSED(未使用),
 * 在秒杀订单中使用时转为 USED(已使用),超期自动转 EXPIRED(已过期)。
 */
@Getter @Setter
@Entity
@Table(name = "landing_user_coupon")
@Erupt(name = "用户优惠券", power = @Power(importable = true, export = true))
public class LandingUserCoupon extends BaseModel {

    @EruptField(views = @View(title = "优惠券模板ID"),
                edit = @Edit(title = "优惠券模板ID", search = @Search))
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @EruptField(views = @View(title = "券名称"),
                edit = @Edit(title = "券名称", show = false))
    @Column(length = 100)
    private String couponName;

    @EruptField(views = @View(title = "优惠码"),
                edit = @Edit(title = "优惠码", search = @Search))
    @Column(name = "coupon_code", length = 20)
    private String couponCode;

    @EruptField(views = @View(title = "手机号"),
                edit = @Edit(title = "手机号", search = @Search))
    @Column(length = 20)
    private String phone;

    @EruptField(views = @View(title = "面值/折扣率"),
                edit = @Edit(title = "面值/折扣率", show = false))
    @Column(name = "value", precision = 10, scale = 2)
    private java.math.BigDecimal value;

    @EruptField(views = @View(title = "使用门槛(元)"),
                edit = @Edit(title = "使用门槛(元)", show = false))
    @Column(name = "min_amount", precision = 10, scale = 2)
    private java.math.BigDecimal minAmount;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "UserCouponStatus")))
    @Column(nullable = false)
    private Integer status = UserCouponStatus.UNUSED.code;

    @EruptField(views = @View(title = "领取时间"),
                edit = @Edit(title = "领取时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "claim_time", nullable = false)
    private LocalDateTime claimTime;

    @EruptField(views = @View(title = "到期时间"),
                edit = @Edit(title = "到期时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @EruptField(views = @View(title = "使用秒杀订单ID"),
                edit = @Edit(title = "使用秒杀订单ID", search = @Search))
    @Column(name = "seckill_order_id")
    private Long seckillOrderId;

    @EruptField(views = @View(title = "使用时间"),
                edit = @Edit(title = "使用时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "use_time")
    private LocalDateTime useTime;
}
