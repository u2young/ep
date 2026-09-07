package xyz.herz.ep.pur.entity.settings;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.pur.core.PurEnumChoiceFetchHandler;
import xyz.herz.ep.pur.enums.PurDictEnums.EnableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 采购配置(单例,参考 ERPNext Buying Settings)。
 * <p>id 固定 1L;启动时由 Initializer 幂等插入;业务方 findFirstByOrderByIdAsc 获取唯一配置。
 */
@Getter @Setter
@Entity
@Table(name = "pur_settings")
@Erupt(name = "采购配置", power = @Power(edit = false))
public class PurPurchaseSettings extends MetaModelVo {

    @EruptField(views = @View(title = "默认采购员"),
                edit = @Edit(title = "默认采购员"))
    @Column(name = "default_buyer", length = 50)
    private String defaultBuyer;

    @EruptField(views = @View(title = "默认前置天数"),
                edit = @Edit(title = "默认前置天数", desc = "采购到货平均天数"))
    @Column(name = "default_lead_days")
    private Integer defaultLeadDays = 7;

    @EruptField(views = @View(title = "默认采购员邮箱"),
                edit = @Edit(title = "默认采购员邮箱"))
    @Column(name = "buyer_email", length = 100)
    private String buyerEmail;

    @EruptField(views = @View(title = "最小订单金额"),
                edit = @Edit(title = "最小订单金额", desc = "低于此金额免询价直接采购"))
    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount = new BigDecimal("1000.00");

    @EruptField(views = @View(title = "是否启用"),
                edit = @Edit(title = "是否启用", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = PurEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EnableStatus")))
    @Column(nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}
