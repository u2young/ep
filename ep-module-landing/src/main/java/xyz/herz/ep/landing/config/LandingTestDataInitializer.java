package xyz.herz.ep.landing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import xyz.herz.ep.landing.entity.LandingCoupon;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.entity.LandingSeckill;
import xyz.herz.ep.landing.entity.LandingSeckillOrder;
import xyz.herz.ep.landing.entity.LandingUserCoupon;
import xyz.herz.ep.landing.enums.LandingDictEnums.CouponStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.PageStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.SeckillStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.UserCouponStatus;
import xyz.herz.ep.landing.jpa.LandingCouponRepository;
import xyz.herz.ep.landing.jpa.LandingPageRepository;
import xyz.herz.ep.landing.jpa.LandingSeckillOrderRepository;
import xyz.herz.ep.landing.jpa.LandingSeckillRepository;
import xyz.herz.ep.landing.jpa.LandingTemplateRepository;
import xyz.herz.ep.landing.jpa.LandingUserCouponRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 落地页模块测试数据种子加载器。
 * <p>启动时若页面表为空,从 classpath 读取 seeds.json 并批量插入测试数据,
 * 让开发/测试环境开箱即用,覆盖以下场景:
 * <ul>
 *   <li>7 个落地页(6 已发布 + 1 草稿)</li>
 *   <li>3 个秒杀活动(进行中/草稿/已结束)</li>
 *   <li>4 个优惠券(启用/草稿/已过期)</li>
 *   <li>4 条用户领券记录(未使用/已使用)</li>
 *   <li>4 条秒杀订单(待处理/已确认/已取消)</li>
 * </ul>
 * 幂等:已有数据则跳过。
 */
@Component
public class LandingTestDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LandingTestDataInitializer.class);
    private static final String SEEDS_PATH = "test-data/landing/seeds.json";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private LandingTemplateRepository tplRepo;
    @Autowired private LandingSeckillRepository seckillRepo;
    @Autowired private LandingSeckillOrderRepository seckillOrderRepo;
    @Autowired private LandingCouponRepository couponRepo;
    @Autowired private LandingUserCouponRepository userCouponRepo;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        if (pageRepo.count() > 0) {
            log.debug("落地页数据已存在({}页),跳过种子加载", pageRepo.count());
            return;
        }
        log.info("加载落地页测试种子数据: {}", SEEDS_PATH);
        try {
            JsonNode root = loadSeeds();
            safeInsert("pages",         () -> insertPages(root.get("pages")));
            safeInsert("seckill",       () -> insertSeckill(root.get("seckill")));
            safeInsert("seckillOrders", () -> insertSeckillOrders(root.get("seckillOrders")));
            safeInsert("coupons",       () -> insertCoupons(root.get("coupons")));
            safeInsert("userCoupons",   () -> insertUserCoupons(root.get("userCoupons")));
            log.info("种子数据加载完成: {}页 {}秒杀 {}订单 {}券 {}用户券",
                    pageRepo.count(), seckillRepo.count(), seckillOrderRepo.count(),
                    couponRepo.count(), userCouponRepo.count());
        } catch (Exception e) {
            log.error("种子数据加载失败", e);
        }
    }

    private void safeInsert(String label, Runnable fn) {
        try {
            fn.run();
        } catch (Exception e) {
            log.warn("种子数据插入 {} 失败,跳过: {}", label, e.getMessage());
        }
    }

    private JsonNode loadSeeds() throws IOException {
        InputStream is = new ClassPathResource(SEEDS_PATH).getInputStream();
        try {
            return objectMapper.readTree(is);
        } finally {
            is.close();
        }
    }

    private void insertPages(JsonNode nodes) {
        for (JsonNode n : nodes) {
            LandingPage p = new LandingPage();
            p.setSlug(n.path("slug").asText());
            p.setName(n.path("name").asText("未命名页面"));
            p.setStatus(n.path("status").asInt(PageStatus.DRAFT.code));
            p.setTemplateId(n.path("templateId").isNull() ? null : n.path("templateId").asLong());
            if (p.getStatus() == PageStatus.PUBLISHED.code) {
                p.setPublishTime(LocalDateTime.now().minusDays(5));
            }
            pageRepo.saveAndFlush(p);
        }
    }

    private void insertSeckill(JsonNode nodes) {
        for (JsonNode n : nodes) {
            LandingSeckill s = new LandingSeckill();
            s.setName(n.path("name").asText());
            s.setProductName(n.path("productName").asText());
            s.setProductDesc(n.path("productDesc").asText(""));
            s.setPrice(new BigDecimal(n.path("price").asText("0")));
            s.setOriginalPrice(new BigDecimal(n.path("originalPrice").asText("0")));
            s.setStock(n.path("stock").asInt(0));
            s.setRemainingStock(n.path("remainingStock").asInt(s.getStock()));
            s.setStartTime(parseTime(n.path("startTime").asText()));
            s.setEndTime(parseTime(n.path("endTime").asText()));
            s.setStatus(n.path("status").asInt(SeckillStatus.DRAFT.code));
            s.setPageId(n.path("pageId").isNull() ? null : n.path("pageId").asLong());
            s.setLimitPerUser(n.path("limitPerUser").asInt(1));
            s.setEnabled(n.path("enabled").asInt(EnableStatus.ENABLED.code));
            s.setRemark(n.path("remark").asText(""));
            seckillRepo.saveAndFlush(s);
        }
    }

    private void insertSeckillOrders(JsonNode nodes) {
        for (JsonNode n : nodes) {
            LandingSeckillOrder o = new LandingSeckillOrder();
            o.setSeckillId(n.path("seckillId").asLong());
            o.setSeckillName(n.path("seckillName").asText());
            o.setProductName(n.path("productName").asText());
            o.setPhone(n.path("phone").asText());
            o.setUserName(n.path("userName").asText(""));
            o.setQuantity(n.path("quantity").asInt(1));
            o.setPaidAmount(new BigDecimal(n.path("paidAmount").asText("0")));
            o.setCouponId(n.path("couponId").isNull() ? null : n.path("couponId").asLong());
            o.setStatus(n.path("status").asInt(0));
            o.setClaimTime(parseTime(n.path("claimTime").asText()));
            o.setConfirmTime(n.path("confirmTime").asText("").isEmpty() ? null : parseTime(n.path("confirmTime").asText()));
            o.setRemark(n.path("remark").asText(""));
            seckillOrderRepo.saveAndFlush(o);
        }
    }

    private void insertCoupons(JsonNode nodes) {
        for (JsonNode n : nodes) {
            LandingCoupon c = new LandingCoupon();
            c.setName(n.path("name").asText());
            c.setCouponCode(n.path("couponCode").asText("").toUpperCase());
            c.setType(n.path("type").asInt(1));
            c.setValue(new BigDecimal(n.path("value").asText("0")));
            c.setMinAmount(new BigDecimal(n.path("minAmount").asText("0")));
            c.setLimitPerUser(n.path("limitPerUser").asInt(1));
            c.setTotalLimit(n.path("totalLimit").asInt(0));
            c.setClaimedCount(n.path("claimedCount").asInt(0));
            c.setStartTime(parseTime(n.path("startTime").asText()));
            c.setEndTime(parseTime(n.path("endTime").asText()));
            c.setValidDays(n.path("validDays").asInt(0));
            c.setStatus(n.path("status").asInt(CouponStatus.DRAFT.code));
            c.setRemark(n.path("remark").asText(""));
            couponRepo.saveAndFlush(c);
        }
    }

    private void insertUserCoupons(JsonNode nodes) {
        for (JsonNode n : nodes) {
            LandingUserCoupon u = new LandingUserCoupon();
            u.setCouponId(n.path("couponId").asLong());
            u.setCouponName(n.path("couponName").asText());
            u.setCouponCode(n.path("couponCode").asText(""));
            u.setPhone(n.path("phone").asText());
            u.setValue(new BigDecimal(n.path("value").asText("0")));
            u.setMinAmount(new BigDecimal(n.path("minAmount").asText("0")));
            u.setStatus(n.path("status").asInt(UserCouponStatus.UNUSED.code));
            u.setClaimTime(parseTime(n.path("claimTime").asText()));
            u.setExpireTime(parseTime(n.path("expireTime").asText()));
            u.setSeckillOrderId(n.path("seckillOrderId").isNull() ? null : n.path("seckillOrderId").asLong());
            u.setUseTime(n.path("useTime").asText("").isEmpty() ? null : parseTime(n.path("useTime").asText()));
            userCouponRepo.saveAndFlush(u);
        }
    }

    private LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(text.trim(), FMT);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
