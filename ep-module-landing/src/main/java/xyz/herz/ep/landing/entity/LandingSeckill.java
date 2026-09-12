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
import xyz.herz.ep.landing.handler.SeckillActiveHandler;
import xyz.herz.ep.landing.handler.SeckillEndHandler;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.SeckillStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 秒杀活动。
 * <p>状态机:0 草稿 → 10 进行中 → 20 已结束;
 * 通过行按钮「开始」/「结束」切换,时间到期自动转已结束。
 */
@Getter @Setter
@Entity
@Table(name = "landing_seckill")
@Erupt(
    name = "秒杀活动",
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(title = "开始", code = SeckillActiveHandler.CODE, icon = "fa fa-play",
            operationHandler = SeckillActiveHandler.class, operationParam = { SeckillActiveHandler.CODE }),
        @RowOperation(title = "结束", code = SeckillEndHandler.CODE, icon = "fa fa-stop",
            operationHandler = SeckillEndHandler.class, operationParam = { SeckillEndHandler.CODE })
    }
)
public class LandingSeckill extends BaseModel {

    @EruptField(views = @View(title = "活动名称"),
                edit = @Edit(title = "活动名称", notNull = true, search = @Search))
    @Column(length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "商品名称"),
                edit = @Edit(title = "商品名称", notNull = true, search = @Search))
    @Column(length = 200, nullable = false)
    private String productName;

    @EruptField(views = @View(title = "商品描述"),
                edit = @Edit(title = "商品描述", type = EditType.TEXTAREA, show = false))
    @Column(columnDefinition = "TEXT")
    private String productDesc;

    @EruptField(views = @View(title = "秒杀价"),
                edit = @Edit(title = "秒杀价(元)", notNull = true, search = @Search,
                    desc = "单位:元,支持小数"))
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price;

    @EruptField(views = @View(title = "原价"),
                edit = @Edit(title = "原价(元)", search = @Search))
    @Column(name = "original_price", precision = 10, scale = 2)
    private java.math.BigDecimal originalPrice;

    @EruptField(views = @View(title = "库存"),
                edit = @Edit(title = "总库存", notNull = true, search = @Search,
                    desc = "秒杀总库存,每笔成功抢购递减"))
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;

    @EruptField(views = @View(title = "剩余库存"),
                edit = @Edit(title = "剩余库存", show = false,
                    desc = "自动计算:stock - 已确认订单数"))
    @Column(name = "remaining_stock", nullable = false)
    private Integer remainingStock = 0;

    @EruptField(views = @View(title = "开始时间"),
                edit = @Edit(title = "开始时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @EruptField(views = @View(title = "结束时间"),
                edit = @Edit(title = "结束时间", notNull = true,
                    dateType = @DateType(type = DateType.Type.DATE_TIME)))
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true, search = @Search,
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "SeckillStatus")))
    @Column(nullable = false)
    private Integer status = SeckillStatus.DRAFT.code;

    @EruptField(views = @View(title = "关联落地页"),
                edit = @Edit(title = "关联落地页", search = @Search,
                    desc = "绑定后在落地页 amis 可用 ${seckill.id} 引用"))
    @Column(name = "page_id")
    private Long pageId;

    @EruptField(views = @View(title = "每人限购"),
                edit = @Edit(title = "每人限购数量",
                    desc = "限制同一手机号最多抢购几件,默认1"))
    @Column(name = "limit_per_user", nullable = false)
    private Integer limitPerUser = 1;

    @EruptField(views = @View(title = "启用"),
                edit = @Edit(title = "启用",
                    choiceType = @ChoiceType(fetchHandler = LandingEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(name = "enabled", nullable = false)
    private Integer enabled = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "说明"),
                edit = @Edit(title = "说明", type = EditType.TEXTAREA, show = false))
    @Column(columnDefinition = "TEXT")
    private String remark;
}
