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
        assertTrue(cnt >= 6, () -> "报表数应 >= 6,实际 " + cnt
            + " (6 模块各 1 张以上: CRM/ERP/Mall×2/WMS/IoT/Landing)");

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
}
