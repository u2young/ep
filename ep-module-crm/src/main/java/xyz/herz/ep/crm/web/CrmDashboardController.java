package xyz.herz.ep.crm.web;

import xyz.erupt.annotation.sub_erupt.Tpl;
import xyz.erupt.tpl.annotation.EruptTpl;
import xyz.erupt.tpl.annotation.TplAction;
import xyz.herz.ep.crm.jpa.CrmClueRepository;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import xyz.herz.ep.crm.jpa.CrmBusinessRepository;
import xyz.herz.ep.crm.jpa.CrmFollowUpRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * CRM 仪表盘 Controller。
 * <p>
 * 使用 {@code @EruptTpl(engine = Tpl.Engine.Thymeleaf)} 注册为 erupt-tpl 模板控制器，
 * 通过 {@code @TplAction(value="dashboard", path="/cr/dashboard")} 暴露仪表盘渲染接口。
 * <p>
 * 统计维度:
 * <ul>
 *   <li>线索总量 / 已跟进 / 已转化</li>
 *   <li>客户总量 / 未成交 / 公海客户</li>
 *   <li>商机总量 / 进行中 / 赢单 / 输单 / 无效</li>
 *   <li>转化率 / 胜率</li>
 * </ul>
 */
@Controller
@EruptTpl(engine = Tpl.Engine.Thymeleaf)
public class CrmDashboardController {

    @Autowired private CrmClueRepository clueRepo;
    @Autowired private CrmCustomerRepository customerRepo;
    @Autowired private CrmBusinessRepository businessRepo;
    @Autowired private CrmFollowUpRecordRepository followRepo;

    /**
     * 仪表盘主页面。
     * erupt-tpl 框架将此方法注册到 /cr/dashboard 路由，渲染 Thymeleaf 模板。
     */
    @TplAction(value = "dashboard", path = "/cr/dashboard")
    public void dashboard(Map<String, Object> model, HttpServletResponse response) throws IOException {
        // ===== 线索统计 =====
        long clueTotal         = clueRepo.count();
        long clueFollowed      = clueRepo.countByFollowUpStatus(1);
        long clueTransformed   = clueRepo.countByTransformStatus(1);

        // ===== 客户统计 =====
        long customerTotal     = customerRepo.count();
        long customerNotDealt  = customerRepo.countByDealStatus(0);
        long customerSea       = customerRepo.countByOwnerUserIdNull();

        // ===== 商机统计 =====
        long businessTotal     = businessRepo.count();
        long businessActive    = businessRepo.countByEndStatusIsNull();
        long businessWon       = businessRepo.countByEndStatus(1);
        long businessLost      = businessRepo.countByEndStatus(2);
        long businessInvalid   = businessRepo.countByEndStatus(3);

        // ===== 转化率 / 胜率 =====
        double transformRate   = clueTotal > 0 ? (clueTransformed * 100.0 / clueTotal) : 0.0;
        long closed            = businessWon + businessLost + businessInvalid;
        double winRate         = closed > 0 ? (businessWon * 100.0 / closed) : 0.0;

        model.put("clueTotal",         clueTotal);
        model.put("clueFollowed",      clueFollowed);
        model.put("clueTransformed",   clueTransformed);
        model.put("customerTotal",     customerTotal);
        model.put("customerNotDealt",  customerNotDealt);
        model.put("customerSea",       customerSea);
        model.put("businessTotal",     businessTotal);
        model.put("businessActive",    businessActive);
        model.put("businessWon",       businessWon);
        model.put("businessLost",      businessLost);
        model.put("businessInvalid",   businessInvalid);
        model.put("transformRate",     String.format("%.1f%%", transformRate));
        model.put("winRate",           String.format("%.1f%%", winRate));
    }
}
