package xyz.herz.ep.hr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.hr.entity.attendance.HrAttendance;
import xyz.herz.ep.hr.entity.department.HrDepartment;
import xyz.herz.ep.hr.entity.designation.HrDesignation;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.hr.entity.leavetype.HrLeaveType;
import xyz.herz.ep.hr.entity.leave.HrLeaveApplication;
import xyz.herz.ep.hr.enums.HrDictEnums.AttendanceStatus;
import xyz.herz.ep.hr.enums.HrDictEnums.EmployeeStatus;
import xyz.herz.ep.hr.enums.HrDictEnums.EnableStatus;
import xyz.herz.ep.hr.enums.HrDictEnums.LeaveStatus;
import xyz.herz.ep.hr.handler.attendance.HrAttendanceCheckInHandler;
import xyz.herz.ep.hr.handler.employee.HrEmployeeLifecycleHandler;
import xyz.herz.ep.hr.handler.leave.HrLeaveLifecycleHandler;
import xyz.herz.ep.hr.jpa.attendance.HrAttendanceRepository;
import xyz.herz.ep.hr.jpa.department.HrDepartmentRepository;
import xyz.herz.ep.hr.jpa.designation.HrDesignationRepository;
import xyz.herz.ep.hr.jpa.employee.HrEmployeeRepository;
import xyz.herz.ep.hr.jpa.leavetype.HrLeaveTypeRepository;
import xyz.herz.ep.hr.jpa.leave.HrLeaveApplicationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 人力资源模块冒烟测试(参考 ERPNext HR DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-F 验收矩阵:
 * <ol>
 *   <li>Hr1 员工生命周期:入职/离职/停用 状态机 + hireDate/resignDate 回写</li>
 *   <li>Hr2 考勤签到/签退 + workHours 派生 + 出勤态不可重复签到</li>
 *   <li>Hr3 请假申请/批准 + totalDays 派生(toDate - fromDate + 1)</li>
 *   <li>Hr4 请假拒绝/取消状态机 + 已拒绝/已取消不可再取消</li>
 *   <li>Hr5 部门树 + 职位主数据 + 员工花名册查询</li>
 *   <li>Hr6 状态机守卫:草稿不可直接离职/停用 + status 字段禁止表单直改</li>
 * </ol>
 *
 * <p>hr 独立可测:部门/职位/请假类型均为本模块主数据;员工用 REF 本模块实体;
 * 审批人用 approverId+approverName 快照,不 REF UPMS。
 */
@SpringBootTest(classes = HrTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class HrSmokeTests {

    @Autowired HrEmployeeRepository empRepo;
    @Autowired HrDepartmentRepository deptRepo;
    @Autowired HrDesignationRepository desigRepo;
    @Autowired HrLeaveTypeRepository leaveTypeRepo;
    @Autowired HrLeaveApplicationRepository leaveRepo;
    @Autowired HrAttendanceRepository attRepo;
    @Autowired HrEmployeeLifecycleHandler empLifecycle;
    @Autowired HrAttendanceCheckInHandler attCheckIn;
    @Autowired HrLeaveLifecycleHandler leaveLifecycle;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private HrDepartment seedDept(String code, String name, Long parentId) {
        HrDepartment d = new HrDepartment();
        d.setCode(code); d.setName(name); d.setParentId(parentId);
        d.setIsGroup(false); d.setStatus(EnableStatus.ENABLED.code);
        return deptRepo.save(d);
    }

    private HrDesignation seedDesig(String code, String name) {
        HrDesignation d = new HrDesignation();
        d.setCode(code); d.setName(name); d.setStatus(EnableStatus.ENABLED.code);
        return desigRepo.save(d);
    }

    private HrLeaveType seedLeaveType(String code, String name, String maxDays, boolean paid) {
        HrLeaveType t = new HrLeaveType();
        t.setCode(code); t.setName(name);
        t.setMaxDays(maxDays == null ? null : new BigDecimal(maxDays));
        t.setIsPaid(paid); t.setStatus(EnableStatus.ENABLED.code);
        return leaveTypeRepo.save(t);
    }

    private HrEmployee buildEmp(String empNo, HrDepartment dept, HrDesignation desig) {
        HrEmployee e = new HrEmployee();
        e.setEmpNo(empNo); e.setName("员工-" + empNo);
        e.setDepartment(dept); e.setDesignation(desig);
        e.setStatus(EmployeeStatus.DRAFT.code);
        e.setHireDate(LocalDate.of(2026, 1, 15));
        return empRepo.save(e);
    }

    private HrEmployee activateEmp(HrEmployee e) {
        String r = empLifecycle.exec(List.of(e), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_ACTIVATE});
        assertTrue(r.contains("成功 1"), "入职应成功,实际=" + r);
        return empRepo.findById(e.getId()).orElseThrow();
    }

    // =================== (1) 员工生命周期:入职/离职/停用 ===================
    @Test
    void hr1_employee_lifecycle() {
        HrDepartment dept = seedDept("D-T-1", "研发部", null);
        HrDesignation desig = seedDesig("J-T-1", "工程师");
        HrEmployee emp = buildEmp("E-T-001", dept, desig);
        assertEquals(EmployeeStatus.DRAFT.code, emp.getStatus());

        // 入职:草稿 → 在职
        HrEmployee active = activateEmp(emp);
        assertEquals(EmployeeStatus.ACTIVE.code, active.getStatus());
        assertNotNull(active.getHireDate(), "入职日期应回写");

        // 离职:在职 → 离职
        String resignR = empLifecycle.exec(List.of(active), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_RESIGN});
        assertTrue(resignR.contains("成功 1"), "离职应成功,实际=" + resignR);
        HrEmployee resigned = empRepo.findById(emp.getId()).orElseThrow();
        assertEquals(EmployeeStatus.RESIGNED.code, resigned.getStatus());
        assertNotNull(resigned.getResignDate(), "离职日期应回写");

        // 停用:另一员工 在职 → 停用
        HrEmployee emp2 = activateEmp(buildEmp("E-T-002", dept, desig));
        String disableR = empLifecycle.exec(List.of(emp2), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_DISABLE});
        assertTrue(disableR.contains("成功 1"), "停用应成功,实际=" + disableR);
        HrEmployee disabled = empRepo.findById(emp2.getId()).orElseThrow();
        assertEquals(EmployeeStatus.DISABLED.code, disabled.getStatus());
    }

    // =================== (2) 考勤签到/签退 + workHours 派生 ===================
    @Test
    void hr2_attendance_check_in_out() {
        HrDepartment dept = seedDept("D-T-2", "产品部", null);
        HrDesignation desig = seedDesig("J-T-2", "产品经理");
        HrEmployee emp = activateEmp(buildEmp("E-T-A01", dept, desig));

        HrAttendance att = new HrAttendance();
        att.setAttNo("ATT-T-001"); att.setEmployee(emp);
        att.setAttDate(LocalDate.of(2026, 8, 31));
        att.setStatus(AttendanceStatus.ABSENT.code);
        att = attRepo.save(att);

        // 签到:缺勤 → 出勤
        String inR = attCheckIn.exec(List.of(att), null,
            new String[]{HrAttendanceCheckInHandler.CODE_CHECK_IN});
        assertTrue(inR.contains("成功 1"), "签到应成功,实际=" + inR);
        HrAttendance checkedIn = attRepo.findById(att.getId()).orElseThrow();
        assertEquals(AttendanceStatus.PRESENT.code, checkedIn.getStatus());
        assertNotNull(checkedIn.getInTime(), "签到时间应回写");

        // 模拟 2 小时前签到,再签退,验证 workHours 派生
        checkedIn.setInTime(LocalDateTime.now().minusHours(2).minusMinutes(10));
        String outR = attCheckIn.exec(List.of(checkedIn), null,
            new String[]{HrAttendanceCheckInHandler.CODE_CHECK_OUT});
        assertTrue(outR.contains("成功 1"), "签退应成功,实际=" + outR);
        HrAttendance checkedOut = attRepo.findById(att.getId()).orElseThrow();
        assertNotNull(checkedOut.getOutTime(), "签退时间应回写");
        assertNotNull(checkedOut.getWorkHours(), "工时应派生");
        assertTrue(checkedOut.getWorkHours().signum() > 0, "工时应 > 0,实际=" + checkedOut.getWorkHours());

        // 出勤态重复签到应被拒
        String reIn = attCheckIn.exec(List.of(checkedOut), null,
            new String[]{HrAttendanceCheckInHandler.CODE_CHECK_IN});
        assertTrue(reIn.contains("失败 1"), "出勤状态重复签到应被拒,实际=" + reIn);
    }

    // =================== (3) 请假申请/批准 + totalDays 派生 ===================
    @Test
    void hr3_leave_apply_approve() {
        HrDepartment dept = seedDept("D-T-3", "市场部", null);
        HrDesignation desig = seedDesig("J-T-3", "市场专员");
        HrEmployee emp = activateEmp(buildEmp("E-T-L01", dept, desig));
        HrLeaveType lt = seedLeaveType("LT-ANNUAL", "年假", "5", true);

        HrLeaveApplication leave = new HrLeaveApplication();
        leave.setLeaveNo("LV-T-001"); leave.setEmployee(emp); leave.setLeaveType(lt);
        leave.setFromDate(LocalDate.of(2026, 9, 1));
        leave.setToDate(LocalDate.of(2026, 9, 3));
        leave.setApproverId(7001L); leave.setApproverName("审批人-张三");
        leave.setStatus(LeaveStatus.DRAFT.code); leave.setReason("年假");
        leave = leaveRepo.save(leave);

        // 申请:草稿 → 已申请,totalDays 派生 = 3
        String applyR = leaveLifecycle.exec(List.of(leave), null,
            new String[]{HrLeaveLifecycleHandler.CODE_APPLY});
        assertTrue(applyR.contains("成功 1"), "申请应成功,实际=" + applyR);
        HrLeaveApplication applied = leaveRepo.findById(leave.getId()).orElseThrow();
        assertEquals(LeaveStatus.APPLIED.code, applied.getStatus());
        assertEquals(0, applied.getTotalDays().compareTo(bd("3.0")), "9/1~9/3 共 3 天,totalDays=3");

        // 批准:已申请 → 已批准
        String apprR = leaveLifecycle.exec(List.of(applied), null,
            new String[]{HrLeaveLifecycleHandler.CODE_APPROVE});
        assertTrue(apprR.contains("成功 1"), "批准应成功,实际=" + apprR);
        HrLeaveApplication approved = leaveRepo.findById(leave.getId()).orElseThrow();
        assertEquals(LeaveStatus.APPROVED.code, approved.getStatus());
    }

    // =================== (4) 请假拒绝/取消状态机 ===================
    @Test
    void hr4_leave_reject_cancel() {
        HrDepartment dept = seedDept("D-T-4", "运营部", null);
        HrDesignation desig = seedDesig("J-T-4", "运营专员");
        HrEmployee emp = activateEmp(buildEmp("E-T-L02", dept, desig));
        HrLeaveType lt = seedLeaveType("LT-SICK", "病假", "15", true);

        // A:申请 → 拒绝
        HrLeaveApplication a = new HrLeaveApplication();
        a.setLeaveNo("LV-T-002"); a.setEmployee(emp); a.setLeaveType(lt);
        a.setFromDate(LocalDate.of(2026, 9, 5)); a.setToDate(LocalDate.of(2026, 9, 5));
        a.setStatus(LeaveStatus.DRAFT.code); a.setReason("病假");
        a = leaveRepo.save(a);
        leaveLifecycle.exec(List.of(a), null, new String[]{HrLeaveLifecycleHandler.CODE_APPLY});
        String rejR = leaveLifecycle.exec(List.of(a), null,
            new String[]{HrLeaveLifecycleHandler.CODE_REJECT});
        assertTrue(rejR.contains("成功 1"), "拒绝应成功,实际=" + rejR);
        HrLeaveApplication rejected = leaveRepo.findById(a.getId()).orElseThrow();
        assertEquals(LeaveStatus.REJECTED.code, rejected.getStatus());

        // 已拒绝不可取消
        String cancelRejected = leaveLifecycle.exec(List.of(rejected), null,
            new String[]{HrLeaveLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelRejected.contains("失败 1"), "已拒绝不可取消,实际=" + cancelRejected);

        // B:申请 → 取消
        HrLeaveApplication b = new HrLeaveApplication();
        b.setLeaveNo("LV-T-003"); b.setEmployee(emp); b.setLeaveType(lt);
        b.setFromDate(LocalDate.of(2026, 9, 10)); b.setToDate(LocalDate.of(2026, 9, 10));
        b.setStatus(LeaveStatus.DRAFT.code); b.setReason("事假");
        b = leaveRepo.save(b);
        leaveLifecycle.exec(List.of(b), null, new String[]{HrLeaveLifecycleHandler.CODE_APPLY});
        String cancelR = leaveLifecycle.exec(List.of(b), null,
            new String[]{HrLeaveLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelR.contains("成功 1"), "取消应成功,实际=" + cancelR);
        HrLeaveApplication cancelled = leaveRepo.findById(b.getId()).orElseThrow();
        assertEquals(LeaveStatus.CANCELLED.code, cancelled.getStatus());

        // 已取消不可再取消
        String reCancel = leaveLifecycle.exec(List.of(cancelled), null,
            new String[]{HrLeaveLifecycleHandler.CODE_CANCEL});
        assertTrue(reCancel.contains("失败 1"), "已取消不可再取消,实际=" + reCancel);
    }

    // =================== (5) 部门树 + 职位主数据 + 员工花名册查询 ===================
    @Test
    void hr5_department_designation_roster() {
        HrDepartment root = seedDept("D-ROOT", "集团总部", null);
        root.setIsGroup(true); deptRepo.save(root);
        HrDepartment sub1 = seedDept("D-SUB1", "研发中心", root.getId());
        HrDepartment sub2 = seedDept("D-SUB2", "测试中心", root.getId());

        HrDesignation eng = seedDesig("J-ENG", "软件工程师");
        HrDesignation qa = seedDesig("J-QA", "测试工程师");

        // 3 个员工挂研发中心,1 个挂测试中心
        activateEmp(buildEmp("E-R-001", sub1, eng));
        activateEmp(buildEmp("E-R-002", sub1, eng));
        activateEmp(buildEmp("E-R-003", sub1, eng));
        activateEmp(buildEmp("E-Q-001", sub2, qa));

        List<HrDepartment> children = deptRepo.findByParentId(root.getId());
        assertEquals(2, children.size(), "集团总部下应有 2 个子部门");

        List<HrEmployee> devEmps = empRepo.findByDepartmentId(sub1.getId());
        assertEquals(3, devEmps.size(), "研发中心应有 3 个员工");

        List<HrEmployee> activeAll = empRepo.findByStatus(EmployeeStatus.ACTIVE.code);
        assertEquals(4, activeAll.size(), "在职员工总数=4");

        List<HrDesignation> desigs = desigRepo.findByStatus(EnableStatus.ENABLED.code);
        assertTrue(desigs.size() >= 2, "启用职位 >= 2");
    }

    // =================== (6) 状态机守卫:非法迁移拒绝 + status 字段禁止表单直改 ===================
    @Test
    void hr6_state_machine_guard() {
        HrDepartment dept = seedDept("D-T-6", "财务部", null);
        HrDesignation desig = seedDesig("J-T-6", "会计");
        HrEmployee emp = buildEmp("E-T-G01", dept, desig);

        // (a) 草稿员工不可直接离职(须先入职)
        String resignDraft = empLifecycle.exec(List.of(emp), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_RESIGN});
        assertTrue(resignDraft.contains("失败 1"), "草稿员工离职应被拒,实际=" + resignDraft);
        HrEmployee stillDraft = empRepo.findById(emp.getId()).orElseThrow();
        assertEquals(EmployeeStatus.DRAFT.code, stillDraft.getStatus());

        // (b) 入职后离职,再离职应被拒
        HrEmployee active = activateEmp(emp);
        empLifecycle.exec(List.of(active), null, new String[]{HrEmployeeLifecycleHandler.CODE_RESIGN});
        HrEmployee resigned = empRepo.findById(emp.getId()).orElseThrow();
        assertEquals(EmployeeStatus.RESIGNED.code, resigned.getStatus());
        String reResign = empLifecycle.exec(List.of(resigned), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_RESIGN});
        assertTrue(reResign.contains("失败 1"), "已离职再离职应被拒,实际=" + reResign);

        // (c) 草稿员工直接停用应被拒
        HrEmployee emp2 = buildEmp("E-T-G02", dept, desig);
        String disableDraft = empLifecycle.exec(List.of(emp2), null,
            new String[]{HrEmployeeLifecycleHandler.CODE_DISABLE});
        assertTrue(disableDraft.contains("失败 1"), "草稿员工停用应被拒,实际=" + disableDraft);

        // (d) DataProxy beforeUpdate:status 字段禁止表单直改(非草稿抛异常)
        HrEmployee.Proxy proxy = new HrEmployee.Proxy();
        HrEmployee draftE = new HrEmployee();
        draftE.setStatus(EmployeeStatus.DRAFT.code);
        assertDoesNotThrow(() -> proxy.beforeUpdate(draftE), "草稿状态允许表单编辑");

        HrEmployee activeE = new HrEmployee();
        activeE.setStatus(EmployeeStatus.ACTIVE.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(activeE),
            "在职状态修改应抛异常:status 禁止表单直改");

        HrLeaveApplication.Proxy leaveProxy = new HrLeaveApplication.Proxy();
        HrLeaveApplication applied = new HrLeaveApplication();
        applied.setStatus(LeaveStatus.APPLIED.code);
        assertThrows(IllegalArgumentException.class, () -> leaveProxy.beforeUpdate(applied),
            "已申请状态修改应抛异常:status 禁止表单直改");
    }
}
