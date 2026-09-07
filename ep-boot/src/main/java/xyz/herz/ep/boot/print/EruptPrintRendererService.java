package xyz.herz.ep.boot.print;

import org.springframework.stereotype.Service;

import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.crm.entity.CrmContract;
import xyz.herz.ep.hr.entity.employee.HrEmployee;
import xyz.herz.ep.pay.entity.slip.PaySalarySlip;
import xyz.herz.ep.pay.entity.slip.PaySalarySlipItem;
import xyz.herz.ep.pur.entity.receipt.PurPurchaseReceipt;
import xyz.herz.ep.pur.entity.receipt.PurReceiptItem;
import xyz.herz.ep.qal.entity.inspection.QalInspection;
import xyz.herz.ep.qal.entity.inspection.QalInspectionItem;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;
import xyz.herz.ep.stk.entity.entry.StkStockEntryItem;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem;
import xyz.herz.ep.fin.entity.invoice.FinPurchaseInvoice;
import xyz.herz.ep.fin.entity.invoice.FinSalesInvoice;
import xyz.herz.ep.mfg.entity.workorder.MfgWorkOrder;
import xyz.herz.ep.proj.entity.project.ProjProject;
import xyz.herz.ep.sup.entity.issue.SupIssue;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;
import xyz.herz.ep.sal.entity.quotation.SalQuotation;
import xyz.herz.ep.sal.entity.quotation.SalQuotationItem;

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

    // =================== Fin 销售发票 ===================
    public String renderSalesInvoice(FinSalesInvoice inv) {
        if (inv == null) {
            throw new IllegalArgumentException("FinSalesInvoice 不能为空(打印前需持久化实体,传入 null 无法渲染销售发票 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  发票号: <b data-field=\"no\">" + safe(inv.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customer\">" + safe(inv.getCustomerName()) + "</b>"
            + "  &nbsp;|&nbsp; 入账日期: <b data-field=\"postingDate\">" + fmtDate(inv.getPostingDate()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">到期日</th><td>" + fmtDate(inv.getDueDate()) + "</td></tr>"
            + "  <tr><th>发票金额</th><td>" + fmtAmt(inv.getTotal()) + "</td></tr>"
            + "  <tr><th>价税合计</th><td>" + fmtAmt(inv.getGrandTotal()) + "</td></tr>"
            + "  <tr><th>已付金额</th><td>" + fmtAmt(inv.getPaidAmount()) + "</td></tr>"
            + "  <tr><th>未付金额</th><td style=\"color:#c0392b;font-weight:600\" data-field=\"outstanding\">"
            +     fmtAmt(inv.getOutstandingAmount()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(inv.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("FIN_SALES_INVOICE", "销售发票 / " + safe(inv.getNo()), body);
    }

    // =================== Fin 采购发票 ===================
    public String renderPurchaseInvoice(FinPurchaseInvoice inv) {
        if (inv == null) {
            throw new IllegalArgumentException("FinPurchaseInvoice 不能为空(打印前需持久化实体,传入 null 无法渲染采购发票 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  发票号: <b data-field=\"no\">" + safe(inv.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 供应商: <b data-field=\"supplier\">" + safe(inv.getSupplierName()) + "</b>"
            + "  &nbsp;|&nbsp; 入账日期: <b data-field=\"postingDate\">" + fmtDate(inv.getPostingDate()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">到期日</th><td>" + fmtDate(inv.getDueDate()) + "</td></tr>"
            + "  <tr><th>发票金额</th><td>" + fmtAmt(inv.getTotal()) + "</td></tr>"
            + "  <tr><th>价税合计</th><td>" + fmtAmt(inv.getGrandTotal()) + "</td></tr>"
            + "  <tr><th>已付金额</th><td>" + fmtAmt(inv.getPaidAmount()) + "</td></tr>"
            + "  <tr><th>未付金额</th><td style=\"color:#c0392b;font-weight:600\" data-field=\"outstanding\">"
            +     fmtAmt(inv.getOutstandingAmount()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(inv.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("FIN_PURCHASE_INVOICE", "采购发票 / " + safe(inv.getNo()), body);
    }

    // =================== Mfg 工单 ===================
    public String renderWorkOrder(MfgWorkOrder wo) {
        if (wo == null) {
            throw new IllegalArgumentException("MfgWorkOrder 不能为空(打印前需持久化实体,传入 null 无法渲染工单 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  工单号: <b data-field=\"no\">" + safe(wo.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 产品: <b data-field=\"product\">"
            +     safe(wo.getProduct() == null ? null : wo.getProduct().getName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(wo.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">计划数量</th><td data-field=\"qty\">" + fmt(wo.getQty()) + "</td></tr>"
            + "  <tr><th>已完工数</th><td data-field=\"producedQty\">" + fmt(wo.getProducedQty()) + "</td></tr>"
            + "  <tr><th>计划开工</th><td>" + fmtDate(wo.getPlannedStart()) + "</td></tr>"
            + "  <tr><th>计划完工</th><td>" + fmtDate(wo.getPlannedEnd()) + "</td></tr>"
            + "  <tr><th>实际开工</th><td>" + fmtDT(wo.getActualStart()) + "</td></tr>"
            + "  <tr><th>实际完工</th><td>" + fmtDT(wo.getActualEnd()) + "</td></tr>"
            + "  <tr><th>领料仓</th><td>"
            +     safe(wo.getSourceWarehouse() == null ? null : wo.getSourceWarehouse().getName()) + "</td></tr>"
            + "  <tr><th>入库仓</th><td>"
            +     safe(wo.getTargetWarehouse() == null ? null : wo.getTargetWarehouse().getName()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(wo.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("MFG_WORK_ORDER", "制造工单 / " + safe(wo.getNo()), body);
    }

    // =================== Proj 项目验收单 ===================
    public String renderProject(ProjProject p) {
        if (p == null) {
            throw new IllegalArgumentException("ProjProject 不能为空(打印前需持久化实体,传入 null 无法渲染项目验收单 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  项目编号: <b data-field=\"no\">" + safe(p.getNo()) + "</b>"
            + "  &nbsp;|&nbsp; 项目名称: <b data-field=\"name\">" + safe(p.getName()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customer\">" + safe(p.getCustomerName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(p.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">项目类型</th><td>" + safe(p.getProjectType()) + "</td></tr>"
            + "  <tr><th>计划开始</th><td>" + fmtDate(p.getExpectedStart()) + "</td></tr>"
            + "  <tr><th>计划结束</th><td>" + fmtDate(p.getExpectedEnd()) + "</td></tr>"
            + "  <tr><th>实际开始</th><td>" + fmtDT(p.getActualStart()) + "</td></tr>"
            + "  <tr><th>实际完工</th><td>" + fmtDT(p.getActualEnd()) + "</td></tr>"
            + "  <tr><th>完工%</th><td data-field=\"percentComplete\">" + fmt(p.getPercentComplete()) + "</td></tr>"
            + "  <tr><th>预计收入</th><td>" + fmtAmt(p.getTotalRevenue()) + "</td></tr>"
            + "  <tr><th>累计成本</th><td data-field=\"totalCost\">" + fmtAmt(p.getTotalCost()) + "</td></tr>"
            + "  <tr><th>毛利</th><td data-field=\"grossMargin\">" + fmtAmt(p.getGrossMargin()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(p.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("PROJ_ACCEPTANCE", "项目验收单 / " + safe(p.getNo()), body);
    }

    // =================== Sup 客户工单 ===================
    public String renderTicket(SupIssue t) {
        if (t == null) {
            throw new IllegalArgumentException("SupIssue 不能为空(打印前需持久化实体,传入 null 无法渲染工单 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  工单主题: <b data-field=\"subject\">" + safe(t.getSubject()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customer\">"
            +     safe(t.getCustomer() == null ? null : t.getCustomer().getName()) + "</b>"
            + "  &nbsp;|&nbsp; 优先级: <b data-field=\"priority\">" + safeObj(t.getPriority()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(t.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">提交人</th><td>" + safe(t.getRaisedBy()) + "</td></tr>"
            + "  <tr><th>分派给</th><td>" + safe(t.getAssigneeName()) + "</td></tr>"
            + "  <tr><th>响应截止</th><td>" + fmtDT(t.getResponseBy()) + "</td></tr>"
            + "  <tr><th>解决截止</th><td>" + fmtDT(t.getResolutionBy()) + "</td></tr>"
            + "  <tr><th>首次响应</th><td>" + fmtDT(t.getFirstResponseAt()) + "</td></tr>"
            + "  <tr><th>解决时间</th><td>" + fmtDT(t.getResolvedAt()) + "</td></tr>"
            + "  <tr><th>关闭时间</th><td>" + fmtDT(t.getClosedAt()) + "</td></tr>"
            + "  <tr><th>SLA 达成</th><td data-field=\"slaFulfilled\">" + safeObj(t.getSlaFulfilled()) + "</td></tr>"
            + "  <tr><th>描述</th><td>" + safe(t.getDescription()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(t.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("SUP_TICKET", "客户工单 Ticket / " + safe(t.getSubject()), body);
    }

    // =================== Ast 固定资产卡片 ===================
    public String renderAssetCard(AstAsset a) {
        if (a == null) {
            throw new IllegalArgumentException("AstAsset 不能为空(打印前需持久化实体,传入 null 无法渲染资产卡片 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  资产编号: <b data-field=\"assetNo\">" + safe(a.getAssetNo()) + "</b>"
            + "  &nbsp;|&nbsp; 资产名称: <b data-field=\"name\">" + safe(a.getName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(a.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">类别</th><td>"
            +     safe(a.getCategory() == null ? null : a.getCategory().getName()) + "</td></tr>"
            + "  <tr><th>位置</th><td>"
            +     safe(a.getLocation() == null ? null : a.getLocation().getName()) + "</td></tr>"
            + "  <tr><th>购入日期</th><td>" + fmtDate(a.getPurchaseDate()) + "</td></tr>"
            + "  <tr><th>采购成本(原值)</th><td data-field=\"purchaseAmount\">"
            +     fmtAmt(a.getPurchaseAmount()) + "</td></tr>"
            + "  <tr><th>残值</th><td>" + fmtAmt(a.getSalvageValue()) + "</td></tr>"
            + "  <tr><th>累计折旧</th><td data-field=\"totalDepreciation\">"
            +     fmtAmt(a.getTotalDepreciation()) + "</td></tr>"
            + "  <tr><th>当前净值</th><td data-field=\"currentValue\">"
            +     fmtAmt(a.getCurrentValue()) + "</td></tr>"
            + "  <tr><th>折旧方法</th><td>" + safeObj(a.getDepreciationMethod()) + "</td></tr>"
            + "  <tr><th>使用年限(月)</th><td>" + safeObj(a.getUsefulLifeMonths()) + "</td></tr>"
            + "  <tr><th>已折旧期数</th><td>" + safeObj(a.getDepreciatedPeriods()) + "</td></tr>"
            + "  <tr><th>保管人</th><td>" + safe(a.getCustodianName()) + "</td></tr>"
            + "  <tr><th>成本中心ID</th><td>" + safeObj(a.getCostCenterId()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(a.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("AST_ASSET_CARD", "固定资产卡片 / " + safe(a.getAssetNo()), body);
    }

    // =================== HR 员工档案 ===================
    public String renderEmployeeProfile(HrEmployee e) {
        if (e == null) {
            throw new IllegalArgumentException("HrEmployee 不能为空(打印前需持久化实体,传入 null 无法渲染员工档案 HTML)");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  工号: <b data-field=\"empNo\">" + safe(e.getEmpNo()) + "</b>"
            + "  &nbsp;|&nbsp; 姓名: <b data-field=\"name\">" + safe(e.getName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(e.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">部门</th><td>"
            +     safe(e.getDepartment() == null ? null : e.getDepartment().getName()) + "</td></tr>"
            + "  <tr><th>职位</th><td>"
            +     safe(e.getDesignation() == null ? null : e.getDesignation().getName()) + "</td></tr>"
            + "  <tr><th>性别</th><td>" + safeObj(e.getGender()) + "</td></tr>"
            + "  <tr><th>出生日期</th><td>" + fmtDate(e.getBirthDate()) + "</td></tr>"
            + "  <tr><th>入职日期</th><td>" + fmtDate(e.getHireDate()) + "</td></tr>"
            + "  <tr><th>离职日期</th><td>" + fmtDate(e.getResignDate()) + "</td></tr>"
            + "  <tr><th>手机</th><td>" + safe(e.getPhone()) + "</td></tr>"
            + "  <tr><th>邮箱</th><td>" + safe(e.getEmail()) + "</td></tr>"
            + "  <tr><th>证件类型</th><td>" + safeObj(e.getIdType()) + "</td></tr>"
            + "  <tr><th>证件号码</th><td>" + safe(e.getIdNo()) + "</td></tr>"
            + "  <tr><th>住址</th><td>" + safe(e.getAddress()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(e.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("HR_EMPLOYEE_PROFILE", "员工档案 / " + safe(e.getEmpNo()), body);
    }

    // =================== Pay 工资单 ===================
    public String renderSalarySlip(PaySalarySlip slip) {
        if (slip == null) {
            throw new IllegalArgumentException("PaySalarySlip 不能为空(打印前需持久化实体,传入 null 无法渲染工资单 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (slip.getItems() != null) {
            for (PaySalarySlipItem it : slip.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getComponentCode()) + "</td>"
                    + "<td>" + safe(it.getComponentName()) + "</td>"
                    + "<td>" + safeObj(it.getComponentType()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getAmount()) + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"5\" style=\"color:#999;text-align:center\">(无明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  工资单号: <b data-field=\"slipNo\">" + safe(slip.getSlipNo()) + "</b>"
            + "  &nbsp;|&nbsp; 员工: <b data-field=\"employee\">"
            +     safe(slip.getEmployee() == null ? null : slip.getEmployee().getName()) + "</b>"
            + "  &nbsp;|&nbsp; 薪酬月份: <b data-field=\"postingMonth\">"
            +     safe(slip.getPostingMonth()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(slip.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>组件编码</th>"
            + "  <th>组件名称</th>"
            + "  <th style=\"width:100px\">类型</th>"
            + "  <th style=\"width:140px;text-align:right\">金额</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<div style=\"margin-top:16px;text-align:right\">"
            + "  应发合计: <b data-field=\"grossPay\">" + fmtAmt(slip.getGrossPay()) + "</b><br>"
            + "  扣款合计: " + fmtAmt(slip.getDeductions())
            + " &nbsp; <b>实发合计: " + fmtAmt(slip.getNetPay()) + "</b><br>"
            + "  工资结构: " + safe(slip.getStructure() == null ? null : slip.getStructure().getName())
            + " &nbsp; 过账日期: " + fmtDate(slip.getPostingDate())
            + " &nbsp; 凭证ID: " + safeObj(slip.getJournalEntryId())
            + "</div>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><tbody>"
            + "  <tr><th style=\"width:160px\">备注</th><td>" + safe(slip.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("PAY_SALARY_SLIP", "工资单 / " + safe(slip.getSlipNo()), body);
    }

    // =================== Qal 质检报告 ===================
    public String renderInspectionReport(QalInspection insp) {
        if (insp == null) {
            throw new IllegalArgumentException("QalInspection 不能为空(打印前需持久化实体,传入 null 无法渲染质检报告 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (insp.getItems() != null) {
            for (QalInspectionItem it : insp.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getCriteriaName()) + "</td>"
                    + "<td>" + safe(it.getSpecText()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getReadingValue()) + "</td>"
                    + "<td style=\"text-align:center\">"
                    +     (it.getIsPass() == null ? "-" : (it.getIsPass() ? "合格" : "不合格"))
                    + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"5\" style=\"color:#999;text-align:center\">(无读数明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  质检单号: <b data-field=\"inspectionNo\">" + safe(insp.getInspectionNo()) + "</b>"
            + "  &nbsp;|&nbsp; 物料: <b data-field=\"item\">"
            +     safe(insp.getItemCode()) + " " + safe(insp.getItemName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(insp.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">来源</th><td>"
            +     safe(insp.getSourceType()) + " / " + safe(insp.getSourceNo()) + "</td></tr>"
            + "  <tr><th>批次号</th><td>" + safe(insp.getBatchNo()) + "</td></tr>"
            + "  <tr><th>质检数量 / 抽检数量</th><td>"
            +     fmt(insp.getQty()) + " / " + fmt(insp.getSampleQty()) + "</td></tr>"
            + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>检验参数</th>"
            + "  <th>规格要求</th>"
            + "  <th style=\"width:140px;text-align:right\">读数值</th>"
            + "  <th style=\"width:100px;text-align:center\">单项判定</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><tbody>"
            + "  <tr><th style=\"width:160px\">检查员</th><td>" + safe(insp.getInspector()) + "</td></tr>"
            + "  <tr><th>检查日期</th><td>" + fmtDate(insp.getInspectedDate()) + "</td></tr>"
            + "  <tr><th>判定时间</th><td>" + fmtDT(insp.getDecidedAt()) + "</td></tr>"
            + "  <tr><th>N/C 单号</th><td>" + safe(insp.getNcNo()) + "</td></tr>"
            + "  <tr><th>备注</th><td>" + safe(insp.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("QAL_INSPECTION_REPORT", "质检报告 / " + safe(insp.getInspectionNo()), body);
    }

    // =================== Pur 采购收货单 ===================
    public String renderPurchaseReceipt(PurPurchaseReceipt rcp) {
        if (rcp == null) {
            throw new IllegalArgumentException("PurPurchaseReceipt 不能为空(打印前需持久化实体,传入 null 无法渲染采购收货单 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (rcp.getItems() != null) {
            for (PurReceiptItem it : rcp.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getItemCode()) + "</td>"
                    + "<td>" + safe(it.getItemName()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getOrderedQty()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getReceivedQty()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getUnitPrice()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getAmount()) + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"7\" style=\"color:#999;text-align:center\">(无明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  收货单号: <b data-field=\"receiptNo\">" + safe(rcp.getReceiptNo()) + "</b>"
            + "  &nbsp;|&nbsp; 供应商: <b data-field=\"supplier\">"
            +     safe(rcp.getSupplierName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(rcp.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">供应商编码</th><td>" + safe(rcp.getSupplierCode()) + "</td></tr>"
            + "  <tr><th>来源报价单</th><td>" + safe(rcp.getSourceQuotationNo()) + "</td></tr>"
            + "  <tr><th>收货日期</th><td>" + fmtDate(rcp.getReceiptDate()) + "</td></tr>"
            + "  <tr><th>收货时间</th><td>" + fmtDT(rcp.getReceivedAt()) + "</td></tr>"
            + "  <tr><th>收货人</th><td>" + safe(rcp.getReceiver()) + "</td></tr>"
            + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>物料编码</th>"
            + "  <th>物料名称</th>"
            + "  <th style=\"width:120px;text-align:right\">应收数量</th>"
            + "  <th style=\"width:120px;text-align:right\">实收数量</th>"
            + "  <th style=\"width:140px;text-align:right\">单价</th>"
            + "  <th style=\"width:140px;text-align:right\">小计</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<div style=\"margin-top:16px;text-align:right\">"
            + "  收货数量合计: <b data-field=\"receivedQty\">" + fmt(rcp.getReceivedQty()) + "</b><br>"
            + "  <b>收货金额合计: " + fmtAmt(rcp.getReceivedAmount()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><tbody>"
            + "  <tr><th style=\"width:160px\">备注</th><td>" + safe(rcp.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("PUR_PURCHASE_ORDER", "采购收货单 / " + safe(rcp.getReceiptNo()), body);
    }

    // =================== Stk 库存出入库单 ===================
    public String renderStockEntry(StkStockEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("StkStockEntry 不能为空(打印前需持久化实体,传入 null 无法渲染库存出入库单 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (entry.getItems() != null) {
            for (StkStockEntryItem it : entry.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getItemCode()) + "</td>"
                    + "<td>" + safe(it.getItemName()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getQty()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getUnitPrice()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getAmount()) + "</td>"
                    + "<td>" + safe(it.getBatchNo()) + "</td>"
                    + "<td>" + safe(it.getSerialNo()) + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"8\" style=\"color:#999;text-align:center\">(无明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  单号: <b data-field=\"entryNo\">" + safe(entry.getEntryNo()) + "</b>"
            + "  &nbsp;|&nbsp; 类型: <b data-field=\"entryType\">" + safeObj(entry.getEntryType()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(entry.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">来源仓</th><td>"
            +     safe(entry.getSourceWarehouseCode()) + " " + safe(entry.getSourceWarehouseName()) + "</td></tr>"
            + "  <tr><th>目标仓</th><td>"
            +     safe(entry.getTargetWarehouseCode()) + " " + safe(entry.getTargetWarehouseName()) + "</td></tr>"
            + "  <tr><th>过账日期</th><td>" + fmtDate(entry.getPostingDate()) + "</td></tr>"
            + "  <tr><th>提交时间</th><td>" + fmtDT(entry.getSubmittedAt()) + "</td></tr>"
            + "  <tr><th>操作人</th><td>" + safe(entry.getOperator()) + "</td></tr>"
            + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>物料编码</th>"
            + "  <th>物料名称</th>"
            + "  <th style=\"width:120px;text-align:right\">数量</th>"
            + "  <th style=\"width:140px;text-align:right\">单价</th>"
            + "  <th style=\"width:140px;text-align:right\">小计</th>"
            + "  <th>批次号</th>"
            + "  <th>序列号</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<div style=\"margin-top:16px;text-align:right\">"
            + "  合计数量: <b data-field=\"totalQty\">" + fmt(entry.getTotalQty()) + "</b><br>"
            + "  <b>合计金额: " + fmtAmt(entry.getTotalAmount()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><tbody>"
            + "  <tr><th style=\"width:160px\">备注</th><td>" + safe(entry.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("STK_STOCK_ENTRY", "库存出入库单 / " + safe(entry.getEntryNo()), body);
    }

    // =================== Sal 报价单 ===================
    public String renderQuotation(SalQuotation q) {
        if (q == null) {
            throw new IllegalArgumentException("SalQuotation 不能为空(打印前需持久化实体,传入 null 无法渲染报价单 HTML)");
        }
        StringBuilder rows = new StringBuilder();
        int idx = 0;
        if (q.getItems() != null) {
            for (SalQuotationItem it : q.getItems()) {
                idx++;
                rows.append("<tr>"
                    + "<td>" + idx + "</td>"
                    + "<td>" + safe(it.getItemCode()) + "</td>"
                    + "<td>" + safe(it.getItemName()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmt(it.getQty()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getUnitPrice()) + "</td>"
                    + "<td style=\"text-align:right\">" + fmtAmt(it.getAmount()) + "</td>"
                    + "</tr>");
            }
        }
        if (idx == 0) {
            rows.append("<tr><td colspan=\"6\" style=\"color:#999;text-align:center\">(无明细)</td></tr>");
        }
        String body = ""
            + "<div class=\"meta\">"
            + "  报价单号: <b data-field=\"no\">" + safe(q.getQuotationNo()) + "</b>"
            + "  &nbsp;|&nbsp; 客户: <b data-field=\"customer\">" + safe(q.getCustomerName()) + "</b>"
            + "  &nbsp;|&nbsp; 状态: <b data-field=\"status\">" + safeObj(q.getStatus()) + "</b>"
            + "</div>"
            + "<table class=\"tbl\"><tbody>"
            + "  <tr><th style=\"width:160px\">客户编码</th><td>" + safe(q.getCustomerCode()) + "</td></tr>"
            + "  <tr><th>订单类型</th><td>" + safeObj(q.getOrderType()) + "</td></tr>"
            + "  <tr><th>预计成交金额</th><td data-field=\"expectedAmount\">" + fmtAmt(q.getExpectedAmount()) + "</td></tr>"
            + "  <tr><th>有效期至</th><td>" + fmtDate(q.getValidDate()) + "</td></tr>"
            + "  <tr><th>创建人</th><td>" + safe(q.getCreatedBy()) + "</td></tr>"
            + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><thead><tr>"
            + "  <th style=\"width:60px\">#</th>"
            + "  <th>物料编码</th>"
            + "  <th>物料名称</th>"
            + "  <th style=\"width:120px;text-align:right\">数量</th>"
            + "  <th style=\"width:140px;text-align:right\">单价</th>"
            + "  <th style=\"width:140px;text-align:right\">小计</th>"
            + "</tr></thead><tbody>" + rows + "</tbody></table>"
            + "<table class=\"tbl\" style=\"margin-top:16px\"><tbody>"
            + "  <tr><th style=\"width:160px\">备注</th><td>" + safe(q.getRemark()) + "</td></tr>"
            + "</tbody></table>";
        return wrap("SAL_QUOTATION", "销售报价单 / " + safe(q.getQuotationNo()), body);
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
