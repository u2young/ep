package xyz.herz.ep.boot.report;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * erupt-report 启动初始化器。
 * <p>
 * 当 ep_report 表为空时,写入 23 张跨模块报表种子 (覆盖 16 个模块: CRM/ERP/Mall×2/WMS/IoT/Landing + Fin×3 + Mfg×2 + Proj×2 + Sup×2 + Ast×2 + HR×1 + Pay×1 + Qal×1 + Pur×1 + Stk×1)。
 * <p>
 * 关键约定(匹配 ep-boot/src/test/resources/application.yml):
 * H2 JDBC URL 含 DATABASE_TO_UPPER=FALSE, 所以 Hibernate ddl-auto 建的表/列全部精确小写,
 * 本文件所有原生 SQL 必须使用全小写标识符(例如 crm_contract / amount / customer_id),
 * 否则报 bad SQL grammar「表不存在」。
 */
@Component
public class EruptReportInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final EruptReportRepository repo;

    public EruptReportInitializer(EruptReportRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repo.count() > 0) return; // 幂等

        List<EruptReportEntity> list = new ArrayList<>();

        // (1) CRM_TOP_CONTRACT: CRM 合同金额 TOP 10
        list.add(build("CRM_TOP_CONTRACT", "CRM 客户合同金额 TOP 10", "CRM", "TABLE",
            "select cc.id as contract_id, cc.amount as amount, cc.signed_date as signed_date"
            + " from crm_contract cc order by cc.amount desc limit 10"));

        // (2) ERP_PURCHASE_TREND: ERP 近 12 月采购金额趋势 (月份聚合)
        // H2 兼容函数: FORMATDATETIME / CURRENT_DATE / INTERVAL
        list.add(build("ERP_PURCHASE_TREND", "ERP 近 12 月采购金额趋势", "ERP", "LINE",
            "select formatdatetime(po.order_time, 'yyyy-MM') as stat_month,"
            + " coalesce(sum(po.total_price), 0) as total_price"
            + " from erp_purchase_order po"
            + " where po.order_time >= current_date - interval '12' month"
            + " group by formatdatetime(po.order_time, 'yyyy-MM')"
            + " order by stat_month"));

        // (3) MALL_GMV_DAILY: Mall GMV 日趋势 (mall_trade_order.pay_amount, pay_time)
        list.add(build("MALL_GMV_DAILY", "Mall GMV 日趋势", "Mall", "BAR",
            "select cast(t.pay_time as date) as stat_day,"
            + " coalesce(sum(t.pay_amount), 0) as gmv_amt"
            + " from mall_trade_order t"
            + " where t.pay_time is not null"
            + " group by cast(t.pay_time as date) order by stat_day desc limit 60"));

        // (4) MALL_ORDER_STATUS: Mall 订单状态饼 (Mall 第 2 张, 确保 ≥2)
        list.add(build("MALL_ORDER_STATUS", "Mall 订单状态分布饼", "Mall", "PIE",
            "select case t.status"
            + " when 0 then 'UNPAID' when 1 then 'UNSHIP'"
            + " when 2 then 'SHIPPED' when 3 then 'DONE'"
            + " when 4 then 'CANCELLED'"
            + " else concat('S', cast(t.status as varchar(10))) end as status_code,"
            + " count(*) as order_count from mall_trade_order t group by t.status"));

        // (5) WMS_WH_STOCK_SUM: 各仓库库存汇总 — wms_warehouse <- wms_zone <- wms_location <- wms_stock
        list.add(build("WMS_WH_STOCK_SUM", "WMS 各仓库库存汇总", "WMS", "TABLE",
            "select w.id as warehouse_id,"
            + " coalesce(sum(s.available_qty), 0) as stock_sum_qty"
            + " from wms_warehouse w"
            + " left join wms_zone z on z.warehouse_id = w.id"
            + " left join wms_location l on l.zone_id = z.id"
            + " left join wms_stock s on s.location_id = l.id"
            + " group by w.id order by stock_sum_qty desc"));

        // (6) IOT_ALARM_LEVEL: IoT 告警级别饼分布
        // level 列是 H2 关键字, 用双引号包裹 level (但因为 DATABASE_TO_UPPER=FALSE,
        // 双引号里仍然要小写)
        list.add(build("IOT_ALARM_LEVEL", "IoT 告警级别饼分布", "IoT", "PIE",
            "select case a.\"level\""
            + " when 1 then 'INFO' when 2 then 'WARNING'"
            + " when 3 then 'SEVERE' when 4 then 'CRITICAL'"
            + " else concat('L', cast(a.\"level\" as varchar(10))) end as lv_code,"
            + " count(*) as alarm_count from iot_alarm a group by a.\"level\""));

        // (7) LANDING_PV_UV: Landing PV/UV + 线索数
        list.add(build("LANDING_PV_UV", "Landing PV/UV + 线索数", "Landing", "LINE",
            "select cast(log.access_time as date) as stat_day,"
            + " count(*) as pv_cnt,"
            + " count(distinct log.client_fp) as uv_cnt,"
            + " (select count(*) from landing_lead ld"
            + "   where cast(ld.submit_time as date) = cast(log.access_time as date)) as lead_cnt"
            + " from landing_access_log log"
            + " group by cast(log.access_time as date) order by stat_day desc limit 60"));

        // (8) FIN_TRIAL_BALANCE: 财务试算平衡表(借/贷合计 by 科目,仅计已提交凭证 status=1)
        list.add(build("FIN_TRIAL_BALANCE", "财务试算平衡表", "Fin", "TABLE",
            "select a.code as code, a.name as name,"
            + " coalesce(sum(ji.debit), 0) as debit_amt,"
            + " coalesce(sum(ji.credit), 0) as credit_amt"
            + " from fin_account a"
            + " left join fin_journal_entry_item ji on ji.account_id = a.id"
            + " left join fin_journal_entry je on je.id = ji.journal_id"
            + " where je.status = 1 or je.id is null"
            + " group by a.id, a.code, a.name order by a.code"));

        // (9) FIN_PROFIT_LOSS: 损益表(收入科目 credit - 支出科目 debit)
        list.add(build("FIN_PROFIT_LOSS", "损益表", "Fin", "TABLE",
            "select a.code as code, a.name as name,"
            + " coalesce(sum(ji.credit), 0) - coalesce(sum(ji.debit), 0) as net_amount"
            + " from fin_account a"
            + " left join fin_journal_entry_item ji on ji.account_id = a.id"
            + " left join fin_journal_entry je on je.id = ji.journal_id"
            + " where (a.account_type = 4 or a.account_type = 5)"
            + " and (je.status = 1 or je.id is null)"
            + " group by a.id, a.code, a.name order by a.code"));

        // (10) FIN_AR_AGING: 应收账龄分桶(current/d30/d60/d90plus)
        // H2 datediff('day', d1, d2) = d2 - d1(单位:天);用 due_date 与 current_date 算逾期天数
        list.add(build("FIN_AR_AGING", "应收账龄分析", "Fin", "TABLE",
            "select case"
            + " when datediff('day', si.due_date, current_date) <= 0 then 'current'"
            + " when datediff('day', si.due_date, current_date) <= 30 then 'd30'"
            + " when datediff('day', si.due_date, current_date) <= 60 then 'd60'"
            + " else 'd90plus' end as bucket,"
            + " count(*) as cnt,"
            + " coalesce(sum(si.outstanding_amount), 0) as amt"
            + " from fin_sales_invoice si"
            + " where si.status in (1, 2, 3)"
            + " group by case"
            + " when datediff('day', si.due_date, current_date) <= 0 then 'current'"
            + " when datediff('day', si.due_date, current_date) <= 30 then 'd30'"
            + " when datediff('day', si.due_date, current_date) <= 60 then 'd60'"
            + " else 'd90plus' end"));

        // (11) MFG_WO_STATUS: 工单状态分布饼(参考 ERPNext Work Order 状态机)
        // status: 0=DRAFT/1=NOT_STARTED/2=IN_PRODUCTION/3=COMPLETED/4=STOPPED/5=CANCELLED
        list.add(build("MFG_WO_STATUS", "制造工单状态分布饼", "Mfg", "PIE",
            "select case wo.status"
            + " when 0 then 'DRAFT' when 1 then 'NOT_STARTED' when 2 then 'IN_PRODUCTION'"
            + " when 3 then 'COMPLETED' when 4 then 'STOPPED' when 5 then 'CANCELLED'"
            + " else concat('S', cast(wo.status as varchar(10))) end as status_code,"
            + " count(*) as wo_count from mfg_work_order wo group by wo.status"));

        // (12) MFG_WO_PROGRESS: 工单进度表(计划/已完工/进度比 + 产品名)
        // 进度比 = produced_qty / qty(无数据时 0);left join erp_product 取产品名
        list.add(build("MFG_WO_PROGRESS", "制造工单进度表", "Mfg", "TABLE",
            "select wo.no as wo_no, p.name as product_name,"
            + " wo.qty as planned_qty, wo.produced_qty as produced_qty,"
            + " case when wo.qty is null or wo.qty = 0 then 0"
            + " else wo.produced_qty / wo.qty end as progress_ratio,"
            + " wo.status as status"
            + " from mfg_work_order wo"
            + " left join erp_product p on p.id = wo.product_id"
            + " order by wo.no"));

        // (13) PROJ_CASH_FLOW: 项目现金流趋势(日聚合:流入/流出/净流)
        // flow_type: 1=流入/2=流出
        list.add(build("PROJ_CASH_FLOW", "项目现金流趋势", "Proj", "LINE",
            "select cast(cf.posting_date as date) as stat_day,"
            + " coalesce(sum(case when cf.flow_type = 1 then cf.amount else 0 end), 0) as inflow_amt,"
            + " coalesce(sum(case when cf.flow_type = 2 then cf.amount else 0 end), 0) as outflow_amt,"
            + " coalesce(sum(case when cf.flow_type = 1 then cf.amount else -cf.amount end), 0) as net_amt"
            + " from proj_cash_flow cf"
            + " group by cast(cf.posting_date as date)"
            + " order by stat_day desc limit 60"));

        // (14) PROJ_TASK_COMPLETION: 项目任务完工率(总任务数 / 已完成数)
        // task.status: 2=已完成
        list.add(build("PROJ_TASK_COMPLETION", "项目任务完工率", "Proj", "BAR",
            "select p.name as project_name, count(t.id) as total,"
            + " sum(case when t.status = 2 then 1 else 0 end) as done"
            + " from proj_project p left join proj_task t on t.project_id = p.id"
            + " group by p.id, p.name"));

        // (15) SUP_SLA_COMPLIANCE: SLA 达成率饼(MET/BREACH)
        // issue.status: 2=已解决/3=已关闭(终态,已计算 slaFulfilled)
        // sla_fulfilled: true=MET;false/null=BREACH(H2 boolean = TRUE 比较,null 视为 not true 走 else)
        list.add(build("SUP_SLA_COMPLIANCE", "SLA 达成率", "Sup", "PIE",
            "select case when i.sla_fulfilled = true then 'MET' else 'BREACH' end as sla_status,"
            + " count(*) as issue_count from sup_issue i"
            + " where i.status in (2, 3)"
            + " group by case when i.sla_fulfilled = true then 'MET' else 'BREACH' end"));

        // (16) SUP_ISSUE_TREND: 工单日趋势(按创建时间聚合)
        // create_time: MetaModelVo 继承字段(snake_case create_time,SpringBoot PhysicalNamingStrategy)
        list.add(build("SUP_ISSUE_TREND", "工单日趋势", "Sup", "LINE",
            "select cast(i.create_time as date) as stat_day,"
            + " count(*) as issue_count"
            + " from sup_issue i"
            + " group by cast(i.create_time as date)"
            + " order by stat_day desc limit 60"));

        // (17) AST_DEPRECIATION_SUMMARY: 资产折旧汇总表(编号/名称/原值/累计折旧/净值/状态)
        // total_depreciation / current_value 在草稿态为 null,用 coalesce 兜底
        list.add(build("AST_DEPRECIATION_SUMMARY", "资产折旧汇总表", "Ast", "TABLE",
            "select a.asset_no as asset_no, a.name as name,"
            + " a.purchase_amount as purchase_amount,"
            + " coalesce(a.total_depreciation, 0) as total_dep,"
            + " coalesce(a.current_value, a.purchase_amount) as net_value,"
            + " a.status as status"
            + " from ast_asset a"
            + " order by a.asset_no"));

        // (18) AST_STATUS_DISTRIBUTION: 资产状态分布饼(草稿/可用/部分折旧/完全折旧/出售/报废)
        // status: 0=DRAFT/1=AVAILABLE/2=PARTIALLY_DEPRECIATED/3=FULLY_DEPRECIATED/4=SOLD/5=SCRAPPED
        list.add(build("AST_STATUS_DISTRIBUTION", "资产状态分布饼", "Ast", "PIE",
            "select case a.status"
            + " when 0 then 'DRAFT' when 1 then 'AVAILABLE'"
            + " when 2 then 'PARTIALLY_DEPRECIATED' when 3 then 'FULLY_DEPRECIATED'"
            + " when 4 then 'SOLD' when 5 then 'SCRAPPED'"
            + " else concat('S', cast(a.status as varchar(10))) end as status_code,"
            + " count(*) as asset_count from ast_asset a group by a.status"));

        // (19) HR_EMPLOYEE_ROSTER: 员工花名册(工号/姓名/部门/职位/状态/入职日期)
        list.add(build("HR_EMPLOYEE_ROSTER", "员工花名册", "HR", "TABLE",
            "select e.emp_no as emp_no, e.name as emp_name,"
            + " d.name as dept_name, g.name as desig_name,"
            + " e.status as status, e.hire_date as hire_date, e.phone as phone"
            + " from hr_employee e"
            + " left join hr_department d on d.id = e.department_id"
            + " left join hr_designation g on g.id = e.designation_id"
            + " order by e.emp_no"));

        // (20) PAY_SALARY_SUMMARY: 薪酬月度汇总(月份/工资单数/应发/扣款/实发)
        list.add(build("PAY_SALARY_SUMMARY", "薪酬月度汇总", "Pay", "TABLE",
            "select s.posting_month as stat_month, count(s.id) as slip_count,"
            + " coalesce(sum(s.gross_pay), 0) as gross_total,"
            + " coalesce(sum(s.deductions), 0) as deduction_total,"
            + " coalesce(sum(s.net_pay), 0) as net_total"
            + " from pay_salary_slip s"
            + " group by s.posting_month"
            + " order by s.posting_month desc"));

        // (21) QAL_PASS_RATE: 质检合格率(按物料: 总检数/合格数/不合格数/合格率)
        // status: 2=合格/3=不合格
        list.add(build("QAL_PASS_RATE", "质检合格率", "Qal", "TABLE",
            "select i.item_code as item_code, i.item_name as item_name,"
            + " count(i.id) as total_count,"
            + " sum(case when i.status = 2 then 1 else 0 end) as pass_count,"
            + " sum(case when i.status = 3 then 1 else 0 end) as fail_count,"
            + " case when count(i.id) = 0 then 0"
            + " else sum(case when i.status = 2 then 1 else 0 end) * 1.0 / count(i.id)"
            + " end as pass_rate"
            + " from qal_inspection i"
            + " group by i.item_code, i.item_name"
            + " order by i.item_code"));

        // (22) PUR_TOP_SUPPLIER: 采购 Top 供应商(按已收货金额汇总 TOP 10)
        // 仅计已收货 status=2,按 supplier_code 汇总收货单数 + 收货金额
        list.add(build("PUR_TOP_SUPPLIER", "采购 Top 供应商", "Pur", "TABLE",
            "select r.supplier_code as supplier_code, r.supplier_name as supplier_name,"
            + " count(r.id) as receipt_count,"
            + " coalesce(sum(r.received_amount), 0) as total_amount"
            + " from pur_receipt r"
            + " where r.status = 2"
            + " group by r.supplier_code, r.supplier_name"
            + " order by total_amount desc limit 10"));

        // (23) STK_VALUATION: 库存出入库流水汇总(按物料: 入库金额/出库金额/净额)
        // entry_type: 1=入库(正)/2=出库(负)/3-5=移库/生产/翻包(中性,不计入净额)
        // 仅计已提交 status=1
        list.add(build("STK_VALUATION", "库存出入库流水汇总", "Stk", "TABLE",
            "select i.item_code as item_code, i.item_name as item_name,"
            + " coalesce(sum(case when e.entry_type = 1 then i.amount else 0 end), 0) as receipt_amt,"
            + " coalesce(sum(case when e.entry_type = 2 then i.amount else 0 end), 0) as issue_amt,"
            + " coalesce(sum(case when e.entry_type = 1 then i.amount else 0 end), 0)"
            + " - coalesce(sum(case when e.entry_type = 2 then i.amount else 0 end), 0) as net_amt"
            + " from stk_entry_item i"
            + " left join stk_entry e on e.id = i.entry_id"
            + " where e.status = 1"
            + " group by i.item_code, i.item_name"
            + " order by i.item_code"));

        repo.saveAll(list);
    }

    private static EruptReportEntity build(String code, String name, String module,
                                           String chartType, String sql) {
        EruptReportEntity r = new EruptReportEntity();
        r.setCode(code);
        r.setName(name);
        r.setBizModule(module);
        r.setChartType(chartType);
        r.setSqlStatement(sql);
        r.setRemark("自动初始化报表 " + module + "/" + code);
        return r;
    }
}
