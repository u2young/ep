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
 * 当 ep_report 表为空时,写入 7 张跨模块报表种子 (覆盖 6 个模块, Mall 占 2 张)。
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
