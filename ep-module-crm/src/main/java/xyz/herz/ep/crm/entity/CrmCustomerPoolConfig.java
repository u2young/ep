package xyz.herz.ep.crm.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import lombok.Getter;
import lombok.Setter;

/**
 * 公海回收配置(租户级单条配置)。
 * 由于是 MVP,当前只做一张标准单条配置表,字段全量开放;后续按部门/按角色差异化配置,直接加表即可。
 */
@Getter
@Setter
@Table(name = "crm_customer_pool_config")
@Entity
@Erupt(name = "公海配置", power = @Power(delete = false, add = false))
public class CrmCustomerPoolConfig extends BaseModel {

    @EruptField(
        views = @View(title = "启用公海"),
        edit = @Edit(title = "启用公海")
    )
    private Boolean enabled = Boolean.TRUE;

    @EruptField(
        views = @View(title = "未跟进放入公海天数"),
        edit = @Edit(title = "未跟进放入公海天数",
            desc = "客户最后跟进时间距今天超过该天数且未锁定且未成交 → 自动掉入公海;0=关闭")
    )
    private Integer contactExpireDays = 30;

    @EruptField(
        views = @View(title = "未成交放入公海天数"),
        edit = @Edit(title = "未成交放入公海天数",
            desc = "成为负责人至今超过该天数且未成交 → 掉入公海;0=关闭")
    )
    private Integer dealExpireDays = 90;

    @EruptField(
        views = @View(title = "每人拥有客户上限"),
        edit = @Edit(title = "每人拥有客户上限", desc = "认领/转移前校验,防止销售囤客户;0=不限制")
    )
    private Integer receiveOwnerCount = 200;

    @EruptField(
        views = @View(title = "每人锁定客户上限"),
        edit = @Edit(title = "每人锁定客户上限", desc = "锁客户不进公海,但得有上限;0=不限制")
    )
    private Integer lockOwnerCount = 20;
}
