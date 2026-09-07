package xyz.herz.ep.sal.entity.salesperson;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import jakarta.persistence.*;
import java.math.BigDecimal;

import xyz.herz.ep.sal.core.SalEnumChoiceFetchHandler;
import xyz.herz.ep.sal.enums.SalDictEnums.EnableStatus;

/**
 * 销售员(参考 ERPNext Sales Person DocType)。
 * <p>主数据:工号/姓名/状态/电话/邮箱/所属部门/销售目标/佣金比例。
 */
@Getter @Setter
@Entity
@Table(name = "sal_sales_person")
@Erupt(name = "销售员", power = @Power(importable = true, export = true))
public class SalSalesPerson extends MetaModelVo {

    @EruptField(views = @View(title = "工号"),
            edit = @Edit(title = "工号", notNull = true, search = @Search))
    @Column(name = "emp_no", length = 50, nullable = false)
    private String empNo;

    @EruptField(views = @View(title = "姓名"),
            edit = @Edit(title = "姓名", notNull = true))
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @EruptField(views = @View(title = "状态"),
            edit = @Edit(title = "状态", notNull = true,
                choiceType = @ChoiceType(fetchHandler = SalEnumChoiceFetchHandler.class,
                    fetchHandlerParams = "EnableStatus")))
    @Column(name = "status", nullable = false)
    private Integer status = EnableStatus.ENABLED.code;

    @EruptField(views = @View(title = "部门"),
            edit = @Edit(title = "部门"))
    @Column(name = "dept_code", length = 50)
    private String deptCode;

    @EruptField(views = @View(title = "电话"),
            edit = @Edit(title = "电话"))
    @Column(name = "phone", length = 20)
    private String phone;

    @EruptField(views = @View(title = "邮箱"),
            edit = @Edit(title = "邮箱"))
    @Column(name = "email", length = 100)
    private String email;

    @EruptField(views = @View(title = "销售目标(月)"),
            edit = @Edit(title = "销售目标(月)"))
    @Column(name = "target_amount", precision = 18, scale = 2)
    private BigDecimal targetAmount;

    @EruptField(views = @View(title = "佣金比例(%)"),
            edit = @Edit(title = "佣金比例(%)"))
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @EruptField(views = @View(title = "备注"),
            edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;
}