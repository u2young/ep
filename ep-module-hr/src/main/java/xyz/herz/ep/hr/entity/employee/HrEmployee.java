package xyz.herz.ep.hr.entity.employee;

import lombok.Getter;
import lombok.Setter;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.ReferenceTableType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.MetaModelVo;
import xyz.herz.ep.hr.core.HrEnumChoiceFetchHandler;
import xyz.herz.ep.hr.core.HrStateDataProxy;
import xyz.herz.ep.hr.handler.employee.HrEmployeeLifecycleHandler;
import xyz.herz.ep.hr.enums.HrDictEnums.EmployeeStatus;
import xyz.herz.ep.hr.enums.HrDictEnums.GenderType;
import xyz.herz.ep.hr.enums.HrDictEnums.IdType;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 员工主档(参考 ERPNext Employee DocType)。
 * <p>状态机:0草稿/1在职/2试用期/3离职/4停用。
 * 部门/职位用 @ManyToOne(optional=true) REF,避免 H2 fixture 顺序耦合。
 */
@Getter @Setter
@Entity
@Table(name = "hr_employee")
@Erupt(
    name = "员工",
    power = @Power(importable = true, export = true),
    dataProxy = HrEmployee.Proxy.class,
    rowOperation = {
        @RowOperation(title = "入职", code = HrEmployeeLifecycleHandler.CODE_ACTIVATE,
            operationHandler = HrEmployeeLifecycleHandler.class),
        @RowOperation(title = "离职", code = HrEmployeeLifecycleHandler.CODE_RESIGN,
            operationHandler = HrEmployeeLifecycleHandler.class),
        @RowOperation(title = "停用", code = HrEmployeeLifecycleHandler.CODE_DISABLE,
            operationHandler = HrEmployeeLifecycleHandler.class)
    }
)
public class HrEmployee extends MetaModelVo {

    @EruptField(views = @View(title = "工号"),
                edit = @Edit(title = "工号", notNull = true, search = @Search))
    @Column(name = "emp_no", length = 50, nullable = false)
    private String empNo;

    @EruptField(views = @View(title = "姓名"),
                edit = @Edit(title = "姓名", notNull = true, search = @Search))
    @Column(length = 50, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_id", nullable = true)
    @EruptField(views = @View(title = "部门", column = "name"),
                edit = @Edit(title = "部门", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.department.HrDepartment department;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "designation_id", nullable = true)
    @EruptField(views = @View(title = "职位", column = "name"),
                edit = @Edit(title = "职位", type = EditType.REFERENCE_TABLE,
                    referenceTableType = @ReferenceTableType(id = "id", label = "name")))
    private xyz.herz.ep.hr.entity.designation.HrDesignation designation;

    @EruptField(views = @View(title = "性别"),
                edit = @Edit(title = "性别", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "GenderType")))
    @Column(nullable = false)
    private Integer gender = GenderType.UNKNOWN.code;

    @EruptField(views = @View(title = "出生日期"),
                edit = @Edit(title = "出生日期"))
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @EruptField(views = @View(title = "入职日期"),
                edit = @Edit(title = "入职日期"))
    @Column(name = "hire_date")
    private LocalDate hireDate;

    @EruptField(views = @View(title = "状态"),
                edit = @Edit(title = "状态", notNull = true,
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "EmployeeStatus")))
    @Column(nullable = false)
    private Integer status = EmployeeStatus.DRAFT.code;

    @EruptField(views = @View(title = "手机"),
                edit = @Edit(title = "手机", search = @Search))
    @Column(length = 30)
    private String phone;

    @EruptField(views = @View(title = "邮箱"),
                edit = @Edit(title = "邮箱"))
    @Column(length = 100)
    private String email;

    @EruptField(views = @View(title = "证件类型"),
                edit = @Edit(title = "证件类型",
                    choiceType = @ChoiceType(fetchHandler = HrEnumChoiceFetchHandler.class,
                        fetchHandlerParams = "IdType")))
    @Column(name = "id_type")
    private Integer idType = IdType.ID_CARD.code;

    @EruptField(views = @View(title = "证件号码"),
                edit = @Edit(title = "证件号码"))
    @Column(name = "id_no", length = 50)
    private String idNo;

    @EruptField(views = @View(title = "住址"),
                edit = @Edit(title = "住址"))
    @Column(length = 300)
    private String address;

    @EruptField(views = @View(title = "离职日期"),
                edit = @Edit(title = "离职日期", show = false))
    @Column(name = "resign_date")
    private LocalDate resignDate;

    @EruptField(views = @View(title = "备注"),
                edit = @Edit(title = "备注"))
    @Column(length = 500)
    private String remark;

    public static class Proxy extends HrStateDataProxy<HrEmployee> {
        @Override protected String stateFieldName() { return "status"; }
    }
}
