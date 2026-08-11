package xyz.herz.ep.crm.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 商机状态组(流程配置表)。不同部门/产品线可以定义不同的阶段流。
 * MVP 仅做一张配置表,后续可加 dept_ids 限制适用部门。
 */
@Table(name = "crm_business_status_type")
@Entity
@Erupt(name = "商机状态组", power = @Power(importable = true, export = true))
public class CrmBusinessStatusType extends BaseModel {

    @EruptField(views = @View(title = "状态组名"), edit = @Edit(title = "状态组名", notNull = true))
    private String name;

    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注"))
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
