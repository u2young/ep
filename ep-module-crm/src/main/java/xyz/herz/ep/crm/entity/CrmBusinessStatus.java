package xyz.herz.ep.crm.entity;

import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 商机阶段(一个状态组下多个阶段,按 sort 排序决定先后)。
 * 典型例子(标准销售流程):
 *   10 需求分析(10%) → 20 方案报价(30%) → 30 商务谈判(60%) → 40 合同签订(90%)
 */
@Table(name = "crm_business_status")
@Entity
@Erupt(name = "商机阶段配置", power = @Power(importable = true, export = true))
public class CrmBusinessStatus extends BaseModel {

    @EruptField(views = @View(title = "状态组ID"), edit = @Edit(title = "状态组ID", notNull = true, search = @Search))
    private Long typeId;

    @EruptField(views = @View(title = "阶段名"), edit = @Edit(title = "阶段名", notNull = true))
    private String name;

    @EruptField(views = @View(title = "赢单率%", sortable = true),
        edit = @Edit(title = "赢单率%", desc = "如 10=10%", type = EditType.NUMBER))
    private BigDecimal percent;

    @EruptField(views = @View(title = "排序", sortable = true), edit = @Edit(title = "排序", notNull = true, desc = "数值越小越靠前"))
    private Integer sort;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
