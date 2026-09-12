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
import xyz.herz.ep.landing.enums.LandingDictEnums.SeckillOrderStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import xyz.erupt.annotation.sub_field.EditType;

/**
 * 秒杀订单。
 * <p>用户成功抢购后生成,初始 PENDING(待处理),
 * 运营确认后转为 CONFIRMED(已确认)并扣减库存;取消时回滚库存。
 */
@Getter @Setter
@Entity
@Table(name = "landing_seckill_order")
@Erupt(name = "秒杀订单", power = @Power(importable = true, export = true))
public class LandingSeckillOrder extends BaseModel {

    @EruptField(views = @View(title = "秒杀活动ID"),
                edit = @Edit(title = "秒杀活动ID", search = @Search))
    @Column(name = "seckill_id", nullable = false)
    private Long seckillId;

    @EruptField(views = @View(title = "活动名称"),
                edit = @Edit(title = "活动名称", show = false))
    @Column(length = 100)
    private String seckillName;

    @EruptField(views = @View(title = "商品名称"),
                edit = @Edit(title = "商品名称", show = false))
    @Column(length = 200)
    private String productName;

    @EruptField(views = @View(title = "手机号"),
                edit = @Edit(title = "手机号", search = @Search))
    @Column(length = 20)
    private String phone;

    @EruptField(views = @View(title = "领取人姓名"),
                edit = @Edit(title = "领取人姓名", search = @Search))
    @Column(length = 50)
    private String userName;

    @EruptField(views = @View(title = "数量"),
                edit = @Edit(title = "数量"))
    @Column(nullable = false)
    private Integer quantity = 1;

    @EruptField(views = @View(title = "实付金额"),
                edit = @Edit(title = "实付金额"))
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private java.math.BigDecimal paidAmount;

    @EruptField(views = @View(title = "优惠券ID"),
                edit = @Edit(title = "优惠券ID", search = @Search))
    @Column(name = "coupon_id")
    private Long couponId;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "SeckillOrderStatus")))
    @Column(nullable = false)
    private Integer status = SeckillOrderStatus.PENDING.code;

    @EruptField(views = @View(title = "抢购时间"),
                edit = @Edit(title = "抢购时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "claim_time", nullable = false)
    private LocalDateTime claimTime;

    @EruptField(views = @View(title = "确认时间"),
                edit = @Edit(title = "确认时间",
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注", type = EditType.TEXTAREA, show = false))
    @Column(columnDefinition = "TEXT")
    private String remark;
}
