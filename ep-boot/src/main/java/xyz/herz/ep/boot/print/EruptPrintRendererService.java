package xyz.herz.ep.boot.print;

import org.springframework.stereotype.Service;

import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * erupt-print 渲染门面(自管轻量实现,类名加 Renderer 后缀避免与 erupt-print 原生
 * {@code xyz.erupt.print.service.EruptPrintService} bean 名/Class 歧义)。
 *
 * <p>设计约束:
 * 1) 所有 renderXxx(entity) 入参为 null 时统一抛 {@link IllegalArgumentException},
 *    保证 TR-8.2 空数据一致行为(拒绝 NPE)。
 * 2) 返回值为完整 HTML 片段;测试时用 {@link String#contains} 断言关键字段。
 */
@Service
public class EruptPrintRendererService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String wrap(String code, String title, String bodyInner) {
        return "<!doctype html><html data-print-code=\"" + code + "\"><head><meta charset=\"utf-8\">"
            + "<title>" + title + "</title>"
            + "<style>body{font-family:system-ui;font-size:14px;padding:24px}"
            + ".tbl{border-collapse:collapse;width:100%}"
            + ".tbl th,.tbl td{border:1px solid #ccc;padding:6px 8px;text-align:left}"
            + ".h{font-size:20px;font-weight:600;border-bottom:2px solid #333;margin-bottom:16px}"
            + ".meta{margin-bottom:20px;color:#555}</style></head><body>"
            + "<div class=\"h\" data-field=\"title\">" + title + "</div>"
            + bodyInner
            + "</body></html>";
    }

    // =================== CRM 合同 ===================
    public String renderContract(CrmContract c) {
        if (c == null) {
            throw new IllegalArgumentException("CrmContract 不能为空(打印前需持久化实体,传入 null 无法渲染合同 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  合同号: <b data-field=\"no\">" + safe(c.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customer\">"
            +     safe(c.getCustomer() == null ? null : c.getCustomer().getName()) + "</b>"
            + "  &nbsp;|&nbsp; 合同金额(元): <b data-field=\"amount\">"
            +     fmtAmt(c.getAmount()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th>合同名称</th><td>" + safe(c.getName()) + "</td></tr>"
            + "  <tr><th>签订日期</th><td>" + fmtDate(c.getSignedDate()) + "</td></tr>"
            + "  <tr><th>服务起始</th><td>" + fmtDate(c.getStartDate()) + "</td></tr>"
            + "  <tr><th>服务结束</th><td>" + fmtDate(c.getEndDate()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(c.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("CRM_CONTRACT", "CRM 合同打印单 / " + safe(c.getNo()), body);
    }

    // =================== ERP 采购单 ===================
    public String renderPurchaseOrder(ErpPurchaseOrder po) {
        if (po == null) {
            throw new IllegalArgumentException("ErpPurchaseOrder 不能为空(打印前需持久化实体,传入 null 无法渲染采购单 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (po.getItems() != null) {
            for (ErpPurchaseOrderItem it : po.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getRemark()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getCount()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getProductPrice()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getTotalPrice()) + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"5\" style=\"color:#999;text-align:center\">(无明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  采购单号: <b data-field=\"no\">" + safe(po.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 下单时间: <b data-field=\"orderTime\">"
            +     fmtDT(po.getOrderTime()) + "</b>"
            + "  &nbsp;|&nbsp; 供应商: <b data-field=\"supplier\">"
            +     safe(po.getSupplier() == null ? null : po.getSupplier().getName()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>明细说明(remark)</th>"
            + "  <th style=\"width:120px;text-align:right\">数量</th>"
            + "  <th style=\"width:140px;text-align:right\">单价</th>"
            + "  <th style=\"width:140px;text-align:right\">小计</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<div style=\"margin-top:16px;text-align:right\">"
            + "  合计数量(totalCount): <b data-field=\"totalCount\">" + fmt(po.getTotalCount()) + "</b><br>"
            + "  商品金额: " + fmtAmt(po.getTotalProductPrice())
            + " &nbsp; 税额: " + fmtAmt(po.getTotalTaxPrice())
            + " &nbsp; <b>订单总金额: " + fmtAmt(po.getTotalPrice()) + "</b>"
            + "</div>";
        return wrap("ERP_PURCHASE_ORDER", "ERP 采购单 / " + safe(po.getNo()), body);
    }

    // =================== WMS 出库通知 ===================
    public String renderShipmentNotice(WmsShipmentNotice sn) {
        if (sn == null) {
            throw new IllegalArgumentException("WmsShipmentNotice 不能为空(打印前需持久化实体,传入 null 无法渲染出库通知 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  通知单号: <b data-field=\"no\">" + safe(sn.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 仓库: <b data-field=\"warehouse\">"
            +     safe(sn.getWarehouse() == null ? null : sn.getWarehouse().getName()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customerName\">"
            +     safe(sn.getCustomerName()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">状态</th><td>" + safeObj(sn.getStatus()) + "</td></tr>"
            + "  <tr><th>备注/明细描述</th><td>" + safe(sn.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("WMS_SHIPMENT_NOTICE", "WMS 出库通知单 / " + safe(sn.getNo()), body);
    }

    // ============ 小工具 ============
    private static String safe(String s) {
        return s == null ? "-" : s;
    }
    private static String safeObj(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
    private static String fmt(BigDecimal b) {
        if (b == null) return "0";
        return b.stripTrailingZeros().toPlainString();
    }
    private static String fmtAmt(BigDecimal b) {
        if (b == null) return "0.00";
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
            return df.format(b);
        } catch (Exception ignore) {
            return b.toPlainString();
        }
    }
    private static String fmtDate(java.time.temporal.TemporalAccessor t) {
        if (t == null) return "-";
        try { return DATE_FMT.format(t); } catch (Exception e) { return String.valueOf(t); }
    }
    private static String fmtDT(java.time.temporal.TemporalAccessor t) {
        if (t == null) return "-";
        try { return DATETIME_FMT.format(t); } catch (Exception e) { return String.valueOf(t); }
    }
}
