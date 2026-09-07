package xyz.herz.ep.boot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 7 / AC-4 erupt-report 集成冒烟测试。
 * <p>
 * 验收点:
 * 1. 启动无异常,Spring Context 正常装载 (保证 pom erupt-report 生效);
 * 2. EruptReportInitializer 作为 ApplicationListener<ApplicationReadyEvent> Bean 存在;
 * 3. EruptReportRepository.count() >= 6 (覆盖 6 个业务模块,每个模块至少 1 张报表);
 * 4. 抽取 3 条 SQL 对 H2 做 EXPLAIN,保证语法 H2 兼容 (无 syntax error)。
 * <p>
 * 说明: 由于 erupt-report 2.1.0 的原生 Bi/BiChart 模型较复杂且依赖 Erupt UPMS 数据表,
 * 这里在 xyz.herz.ep.boot.report 包下自管轻量 EruptReportEntity + JPA Repository,
 * 满足「6 张报表 SQL + count≥6 + H2 EXPLAIN 通过」的 AC-4 局部验收。
 */
@SpringBootTest(classes = EruptBusinessApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
class EruptReportSmokeTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // =================== (1) Initializer 存在 + 作为 Spring Bean 注册 ===================
    @Test
    void report_initializer_exists_in_context() throws Exception {
        Class<?> initCls;
        try {
            initCls = Class.forName("xyz.herz.ep.boot.report.EruptReportInitializer");
        } catch (ClassNotFoundException e) {
            fail("缺少报表初始化器: xyz.herz.ep.boot.report.EruptReportInitializer");
            return;
        }
        Object bean = applicationContext.getBean(initCls);
        assertNotNull(bean, "EruptReportInitializer 应注册为 Spring Bean(ApplicationReadyEvent 触发 insert)");
    }

    // =================== (2) Repository.count() >= 6 报表 ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_count_at_least_six() throws Exception {
        Class<?> repoCls;
        try {
            repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        } catch (ClassNotFoundException e) {
            fail("缺少报表 Repository: xyz.herz.ep.boot.report.EruptReportRepository");
            return;
        }
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method countM = repoCls.getMethod("count");
        Long cnt = (Long) countM.invoke(repo);
        assertTrue(cnt >= 23, () -> "报表数应 >= 23,实际 " + cnt
            + " (16 模块: CRM/ERP/Mall×2/WMS/IoT/Landing + Fin×3 + Mfg×2 + Proj×2 + Sup×2 + Ast×2 + HR×1 + Pay×1 + Qal×1 + Pur×1 + Stk×1)");

        // 抽样:至少包含 CRM_TOP_CONTRACT / ERP_PURCHASE_TREND / MALL_GMV_DAILY 三个 code
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        java.util.List<Object> all = (java.util.List<Object>) findAllM.invoke(repo);
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            codes.add((String) getCode.invoke(row));
        }
        assertTrue(codes.contains("CRM_TOP_CONTRACT"), "应包含 CRM 客户合同金额 TOP10 报表,现有 codes=" + codes);
        assertTrue(codes.contains("ERP_PURCHASE_TREND"), "应包含 ERP 近 12 月采购趋势报表,现有 codes=" + codes);
        assertTrue(codes.contains("MALL_GMV_DAILY"), "应包含 Mall GMV 日趋势报表,现有 codes=" + codes);
        assertTrue(codes.contains("WMS_WH_STOCK_SUM"), "应包含 WMS 各仓库库存汇总报表,现有 codes=" + codes);
        assertTrue(codes.contains("IOT_ALARM_LEVEL"), "应包含 IoT 告警级别分布报表,现有 codes=" + codes);
        assertTrue(codes.contains("LANDING_PV_UV"), "应包含 Landing PV/UV + 转化率报表,现有 codes=" + codes);
        assertTrue(codes.contains("FIN_TRIAL_BALANCE"), "应包含 财务试算平衡表报表,现有 codes=" + codes);
        assertTrue(codes.contains("FIN_PROFIT_LOSS"), "应包含 损益表报表,现有 codes=" + codes);
        assertTrue(codes.contains("FIN_AR_AGING"), "应包含 应收账龄分析报表,现有 codes=" + codes);
        assertTrue(codes.contains("MFG_WO_STATUS"), "应包含 制造工单状态分布报表,现有 codes=" + codes);
        assertTrue(codes.contains("MFG_WO_PROGRESS"), "应包含 制造工单进度表报表,现有 codes=" + codes);
        assertTrue(codes.contains("PROJ_CASH_FLOW"), "应包含 项目现金流趋势报表,现有 codes=" + codes);
        assertTrue(codes.contains("PROJ_TASK_COMPLETION"), "应包含 项目任务完工率报表,现有 codes=" + codes);
        assertTrue(codes.contains("SUP_SLA_COMPLIANCE"), "应包含 SLA 达成率报表,现有 codes=" + codes);
        assertTrue(codes.contains("SUP_ISSUE_TREND"), "应包含 工单日趋势报表,现有 codes=" + codes);
        assertTrue(codes.contains("AST_DEPRECIATION_SUMMARY"), "应包含 资产折旧汇总表报表,现有 codes=" + codes);
        assertTrue(codes.contains("AST_STATUS_DISTRIBUTION"), "应包含 资产状态分布饼报表,现有 codes=" + codes);
        assertTrue(codes.contains("HR_EMPLOYEE_ROSTER"), "应包含 员工花名册报表,现有 codes=" + codes);
        assertTrue(codes.contains("PAY_SALARY_SUMMARY"), "应包含 薪酬月度汇总报表,现有 codes=" + codes);
        assertTrue(codes.contains("QAL_PASS_RATE"), "应包含 质检合格率报表,现有 codes=" + codes);
        assertTrue(codes.contains("PUR_TOP_SUPPLIER"), "应包含 采购 Top 供应商报表,现有 codes=" + codes);
        assertTrue(codes.contains("STK_VALUATION"), "应包含 库存出入库流水汇总报表,现有 codes=" + codes);
        assertTrue(codes.contains("SAL_TOP_SALESPERSON"), "应包含 销售 Top 销售员报表,现有 codes=" + codes);
    }

    // =================== (3) 3 条 SQL EXPLAIN: H2 兼容,无 syntax error ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_h2_compat_three_samples() throws Exception {
        // (A) 先找到 SQL
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String,String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // (B) 抽样 3 条: CRM_TOP_CONTRACT / MALL_GMV_DAILY / IOT_ALARM_LEVEL
        String[] samples = { "CRM_TOP_CONTRACT", "MALL_GMV_DAILY", "IOT_ALARM_LEVEL" };
        for (String code : samples) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            // H2 EXPLAIN 格式: EXPLAIN { SELECT ... }
            // 只要不抛 BadSqlGrammar 就表示 H2 语法兼容
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (4) Fin 3 条报表 SQL EXPLAIN: H2 兼容(表/列对齐 fin_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_fin_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Fin 3 张报表: 试算平衡表 / 损益表 / 应收账龄
        String[] finCodes = { "FIN_TRIAL_BALANCE", "FIN_PROFIT_LOSS", "FIN_AR_AGING" };
        for (String code : finCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (5) Mfg 2 张报表 SQL EXPLAIN: H2 兼容(表/列对齐 mfg_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_mfg_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Mfg 2 张报表: 工单状态分布饼 / 工单进度表
        String[] mfgCodes = { "MFG_WO_STATUS", "MFG_WO_PROGRESS" };
        for (String code : mfgCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (6) Proj 2 张报表 SQL EXPLAIN: H2 兼容(表/列对齐 proj_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_proj_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Proj 2 张报表: 现金流趋势 / 任务完工率
        String[] projCodes = { "PROJ_CASH_FLOW", "PROJ_TASK_COMPLETION" };
        for (String code : projCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (7) Sup 2 张报表 SQL EXPLAIN: H2 兼容(表/列对齐 sup_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_sup_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Sup 2 张报表: SLA 达成率 / 工单日趋势
        String[] supCodes = { "SUP_SLA_COMPLIANCE", "SUP_ISSUE_TREND" };
        for (String code : supCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (8) Ast 2 张报表 SQL EXPLAIN: H2 兼容(表/列对齐 ast_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_ast_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Ast 2 张报表: 资产折旧汇总表 / 资产状态分布饼
        String[] astCodes = { "AST_DEPRECIATION_SUMMARY", "AST_STATUS_DISTRIBUTION" };
        for (String code : astCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (9) HR 报表 SQL EXPLAIN: H2 兼容(表/列对齐 hr_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_hr_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // HR 1 张报表: 员工花名册
        String[] hrCodes = { "HR_EMPLOYEE_ROSTER" };
        for (String code : hrCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (10) Pur 报表 SQL EXPLAIN: H2 兼容(表/列对齐 pur_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_pur_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Pur 1 张报表: 采购 Top 供应商
        String[] purCodes = { "PUR_TOP_SUPPLIER" };
        for (String code : purCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (11) Stk 报表 SQL EXPLAIN: H2 兼容(表/列对齐 stk_* 实体) ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_stk_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        // Stk 1 张报表: 库存出入库流水汇总
        String[] stkCodes = { "STK_VALUATION" };
        for (String code : stkCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }

    // =================== (12) Sal 报表 SQL EXPLAIN: H2 兼容 ===================
    @Test
    @SuppressWarnings("unchecked")
    void report_sql_explain_sal_reports_h2_compat() throws Exception {
        Class<?> repoCls = Class.forName("xyz.herz.ep.boot.report.EruptReportRepository");
        Object repo = applicationContext.getBean(repoCls);
        java.lang.reflect.Method findAllM = repoCls.getMethod("findAll");
        List<Object> all = (List<Object>) findAllM.invoke(repo);
        java.util.Map<String, String> code2Sql = new java.util.HashMap<>();
        for (Object row : all) {
            java.lang.reflect.Method getCode = row.getClass().getMethod("getCode");
            java.lang.reflect.Method getSql = row.getClass().getMethod("getSqlStatement");
            code2Sql.put((String) getCode.invoke(row), (String) getSql.invoke(row));
        }

        String[] salCodes = { "SAL_TOP_SALESPERSON" };
        for (String code : salCodes) {
            String sql = code2Sql.get(code);
            assertNotNull(sql, () -> code + " 的 SQL 不应为空,现有=" + code2Sql.keySet());
            try {
                List<Map<String, Object>> plan = jdbcTemplate.queryForList("EXPLAIN " + sql);
                assertFalse(plan.isEmpty(), () -> code + " EXPLAIN 应返回至少 1 行 plan,sql=" + sql);
            } catch (Exception ex) {
                fail(code + " H2 EXPLAIN 语法错误: " + ex.getMessage() + "  SQL=" + sql);
            }
        }
    }
}
