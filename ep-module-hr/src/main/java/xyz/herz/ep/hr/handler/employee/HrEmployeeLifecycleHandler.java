package xyz.herz.ep.hr.handler.employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.hr.enums.HrDictEnums.EmployeeStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工生命周期行按钮处理器(参考 ERPNext Employee 状态迁移)。
 * <p>入职(DRAFT→ACTIVE/PROBATION) / 离职(ACTIVE|PROBATION→RESIGNED) / 停用(ACTIVE|PROBATION→DISABLED)。
 */
@Component
public class HrEmployeeLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_ACTIVATE = "hr.employee.activate";
    public static final String CODE_RESIGN = "hr.employee.resign";
    public static final String CODE_DISABLE = "hr.employee.disable";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_ACTIVATE;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof HrEmployee doc)) {
                fail++; sb.append("仅支持员工实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_ACTIVATE -> applyActivate(doc);
                    case CODE_RESIGN -> applyResign(doc);
                    case CODE_DISABLE -> applyDisable(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("员工#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 入职:草稿 → 在职(默认 ACTIVE,试用期可在表单设 PROBATION 后入职)。 */
    private void applyActivate(HrEmployee doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != EmployeeStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿员工可入职,当前状态码: " + st);
        }
        // 若表单已设为试用期则保留试用期,否则默认在职
        if (doc.getStatus() == EmployeeStatus.DRAFT.code) {
            doc.setStatus(EmployeeStatus.ACTIVE.code);
        }
        if (doc.getHireDate() == null) {
            doc.setHireDate(LocalDate.now());
        }
    }

    /** 离职:在职/试用期 → 离职,记录离职日期。 */
    private void applyResign(HrEmployee doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != EmployeeStatus.ACTIVE.code && st != EmployeeStatus.PROBATION.code) {
            throw new IllegalStateException("仅在职/试用期员工可离职,当前状态码: " + st);
        }
        doc.setStatus(EmployeeStatus.RESIGNED.code);
        doc.setResignDate(LocalDate.now());
    }

    /** 停用:在职/试用期 → 停用(长期休假/冻结,非离职)。 */
    private void applyDisable(HrEmployee doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != EmployeeStatus.ACTIVE.code && st != EmployeeStatus.PROBATION.code) {
            throw new IllegalStateException("仅在职/试用期员工可停用,当前状态码: " + st);
        }
        doc.setStatus(EmployeeStatus.DISABLED.code);
    }
}
