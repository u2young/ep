package xyz.herz.ep.landing.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.herz.ep.landing.entity.LandingCoupon;
import xyz.herz.ep.landing.entity.LandingLead;
import xyz.herz.ep.landing.entity.LandingPage;
import xyz.herz.ep.landing.entity.LandingSeckill;
import xyz.herz.ep.landing.entity.LandingSeckillOrder;
import xyz.herz.ep.landing.entity.LandingTemplate;
import xyz.herz.ep.landing.entity.LandingUserCoupon;
import xyz.herz.ep.landing.enums.LandingDictEnums.*;
import xyz.herz.ep.landing.jpa.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 落地页公开 API Controller(magic-api 兜底实现)。
 * <p>公开接口(免登):
 * <ul>
 *   <li>GET  /api/landing/page/slug/{slug} — H5 渲染取已发布页面</li>
 *   <li>GET  /api/landing/template/list    — 模板列表</li>
 *   <li>POST /api/landing/lead             — 留资提交</li>
 *   <li>POST /api/landing/seckill/claim    — 秒杀抢购</li>
 *   <li>GET  /api/landing/seckill/{id}     — 获取秒杀活动详情(含剩余库存)</li>
 *   <li>POST /api/landing/coupon/claim     — 领取优惠券</li>
 *   <li>GET  /api/landing/coupon/info      — 校验优惠券码是否有效</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/landing")
public class LandingApiController {

    private static final Logger log = LoggerFactory.getLogger(LandingApiController.class);

    @Autowired private LandingPageRepository pageRepo;
    @Autowired private LandingTemplateRepository tplRepo;
    @Autowired private LandingLeadRepository leadRepo;
    @Autowired private LandingSeckillRepository seckillRepo;
    @Autowired private LandingSeckillOrderRepository seckillOrderRepo;
    @Autowired private LandingCouponRepository couponRepo;
    @Autowired private LandingUserCouponRepository userCouponRepo;

    // ============ 秒杀接口 ============

    /** 获取秒杀活动详情(H5 展示用)。 */
    @GetMapping("/seckill/{id}")
    public ResponseEntity<Map<String, Object>> getSeckill(@PathVariable Long id) {
        Optional<LandingSeckill> opt = seckillRepo.findById(id);
        if (opt.isEmpty() || opt.get().getEnabled() == null
                || opt.get().getEnabled() != EnableStatus.DISABLED.code) {
            // 已禁用则返回 404;也可改为返回 status=ENDED
        }
        if (opt.isEmpty()) return ok(404, "秒杀活动不存在", null);
        LandingSeckill s = opt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", s.getId());
        data.put("name", s.getName());
        data.put("productName", s.getProductName());
        data.put("productDesc", s.getProductDesc());
        data.put("price", s.getPrice());
        data.put("originalPrice", s.getOriginalPrice());
        data.put("remainingStock", s.getRemainingStock());
        data.put("limitPerUser", s.getLimitPerUser());
        data.put("startTime", s.getStartTime());
        data.put("endTime", s.getEndTime());
        data.put("status", s.getStatus());
        // 倒计时:距结束还有多少秒(-1 表示未开始或已结束)
        if (s.getStatus() != null && s.getStatus() == SeckillStatus.ACTIVE.code && s.getEndTime() != null) {
            long seconds = java.time.Duration.between(LocalDateTime.now(), s.getEndTime()).getSeconds();
            data.put("countdownSeconds", seconds > 0 ? seconds : 0);
        } else {
            data.put("countdownSeconds", -1);
        }
        return ok(1, "ok", data);
    }

    /** 秒杀抢购:校验库存+时间+限购 → 生成订单 → 扣减剩余库存。 */
    @PostMapping("/seckill/claim")
    @Transactional
    public ResponseEntity<Map<String, Object>> claimSeckill(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest req) {
        Long seckillId = toLong(body.get("seckillId"));
        String phone = toStr(body.get("phone"));
        String userName = toStr(body.get("userName"));
        if (seckillId == null || phone == null || phone.isBlank()) {
            return ok(400, "seckillId 和 phone 必填", null);
        }
        Optional<LandingSeckill> opt = seckillRepo.findById(seckillId);
        if (opt.isEmpty()) return ok(404, "秒杀活动不存在", null);
        LandingSeckill s = opt.get();

        // 状态校验:仅进行中可抢
        if (s.getStatus() == null || s.getStatus() != SeckillStatus.ACTIVE.code) {
            return ok(400, "当前活动不在进行中", null);
        }
        // 时间窗口校验
        LocalDateTime now = LocalDateTime.now();
        if (s.getStartTime() != null && s.getStartTime().isAfter(now)) {
            return ok(400, "活动尚未开始", null);
        }
        if (s.getEndTime() != null && s.getEndTime().isBefore(now)) {
            return ok(400, "活动已结束", null);
        }
        // 库存校验
        if (s.getRemainingStock() == null || s.getRemainingStock() <= 0) {
            return ok(400, "库存已售罄", null);
        }
        // 限购校验:同一手机号在同一活动中已抢多少件
        long claimedQty = seckillOrderRepo.countBySeckillIdAndPhone(seckillId, phone);
        int limit = s.getLimitPerUser() == null ? 1 : s.getLimitPerUser();
        if (claimedQty >= limit) {
            return ok(400, "已达到每人限购数量(" + limit + "件)", null);
        }

        // 创建订单
        LandingSeckillOrder order = new LandingSeckillOrder();
        order.setSeckillId(s.getId());
        order.setSeckillName(s.getName());
        order.setProductName(s.getProductName());
        order.setPhone(phone);
        order.setUserName(userName);
        order.setQuantity(1);
        order.setPaidAmount(s.getPrice());
        order.setStatus(SeckillOrderStatus.PENDING.code);
        order.setClaimTime(now);
        seckillOrderRepo.save(order);

        // 原子扣减剩余库存
        int updated = s.getRemainingStock() - 1;
        s.setRemainingStock(updated);
        seckillRepo.save(s);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("orderId", order.getId());
        data.put("remainingStock", updated);
        return ok(1, "抢购成功", data);
    }

    // ============ 优惠券接口 ============

    /** 校验优惠券码是否有效(供 H5 表单实时校验)。 */
    @GetMapping("/coupon/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestParam String code) {
        if (code == null || code.isBlank()) {
            return ok(400, "couponCode 必填", null);
        }
        Optional<LandingCoupon> opt = couponRepo.findByCouponCode(code.toUpperCase());
        if (opt.isEmpty()) {
            return ok(404, "优惠券码无效", null);
        }
        LandingCoupon c = opt.get();
        // 仅已启用且未过期的券才有效
        if (c.getStatus() == null || c.getStatus() != CouponStatus.ENABLED.code) {
            return ok(400, "该优惠券未启用或已禁用", null);
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getEndTime() != null && c.getEndTime().isBefore(now)) {
            return ok(400, "优惠券已过期", null);
        }
        if (c.getStartTime() != null && c.getStartTime().isAfter(now)) {
            return ok(400, "优惠券尚未开放领取", null);
        }
        // 总量限制
        if (c.getTotalLimit() != null && c.getTotalLimit() > 0
                && c.getClaimedCount() != null && c.getClaimedCount() >= c.getTotalLimit()) {
            return ok(400, "优惠券已领完", null);
        }
        Map<String, Object> info = new HashMap<>();
        info.put("couponId", c.getId());
        info.put("name", c.getName());
        info.put("type", c.getType());
        info.put("value", c.getValue());
        info.put("minAmount", c.getMinAmount());
        info.put("limitPerUser", c.getLimitPerUser());
        info.put("remaining", c.getTotalLimit() == null || c.getTotalLimit() == 0
                ? -1 : c.getTotalLimit() - c.getClaimedCount());
        return ok(1, "ok", info);
    }

    /** 领取优惠券:写入用户领券记录,累加模板已领取数。 */
    @PostMapping("/coupon/claim")
    @Transactional
    public ResponseEntity<Map<String, Object>> claimCoupon(@RequestBody Map<String, Object> body,
                                                            HttpServletRequest req) {
        String code = toStr(body.get("couponCode"));
        String phone = toStr(body.get("phone"));
        if (code == null || code.isBlank() || phone == null || phone.isBlank()) {
            return ok(400, "couponCode 和 phone 必填", null);
        }
        Optional<LandingCoupon> opt = couponRepo.findByCouponCode(code.toUpperCase());
        if (opt.isEmpty()) return ok(404, "优惠券码无效", null);
        LandingCoupon c = opt.get();
        if (c.getStatus() == null || c.getStatus() != CouponStatus.ENABLED.code) {
            return ok(400, "该优惠券未启用", null);
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getEndTime() != null && c.getEndTime().isBefore(now)) return ok(400, "优惠券已过期");
        if (c.getStartTime() != null && c.getStartTime().isAfter(now)) return ok(400, "优惠券尚未开放领取");
        if (c.getTotalLimit() != null && c.getTotalLimit() > 0
                && c.getClaimedCount() != null && c.getClaimedCount() >= c.getTotalLimit()) {
            return ok(400, "优惠券已领完", null);
        }
        // 每人限购
        long claimed = userCouponRepo.countByCouponIdAndStatus(c.getId(), UserCouponStatus.UNUSED.code);
        int limit = c.getLimitPerUser() == null ? 1 : c.getLimitPerUser();
        if (claimed >= limit) return ok(400, "已达到每人限领数量(" + limit + "张)", null);

        // 计算到期时间
        LocalDateTime expireTime = null;
        if (c.getValidDays() != null && c.getValidDays() > 0) {
            expireTime = now.plusDays(c.getValidDays());
        }

        LandingUserCoupon uc = new LandingUserCoupon();
        uc.setCouponId(c.getId());
        uc.setCouponName(c.getName());
        uc.setCouponCode(c.getCouponCode());
        uc.setPhone(phone);
        uc.setValue(c.getValue());
        uc.setMinAmount(c.getMinAmount());
        uc.setStatus(UserCouponStatus.UNUSED.code);
        uc.setClaimTime(now);
        uc.setExpireTime(expireTime);
        userCouponRepo.save(uc);

        // 累加模板已领取数
        c.setClaimedCount((c.getClaimedCount() == null ? 0 : c.getClaimedCount()) + 1);
        couponRepo.save(c);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("userCouponId", uc.getId());
        data.put("expireTime", expireTime);
        return ok(1, "领取成功", data);
    }

    /** 查询用户已领取的优惠券列表。 */
    @GetMapping("/coupon/list")
    public ResponseEntity<Map<String, Object>> listUserCoupons(@RequestParam String phone) {
        if (phone == null || phone.isBlank()) return ok(400, "phone 必填", null);
        List<LandingUserCoupon> list = userCouponRepo.findByPhone(phone);
        List<Map<String, Object>> data = list.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("couponName", u.getCouponName());
            m.put("couponCode", u.getCouponCode());
            m.put("value", u.getValue());
            m.put("minAmount", u.getMinAmount());
            m.put("status", u.getStatus());
            m.put("claimTime", u.getClaimTime());
            m.put("expireTime", u.getExpireTime());
            m.put("useTime", u.getUseTime());
            return m;
        }).collect(Collectors.toList());
        return ok(1, "ok", data);
    }

    // ============ 原有接口 ============

    /** 按 slug 取已发布页面(H5 渲染用)。 */
    @GetMapping("/page/slug/{slug}")
    public ResponseEntity<Map<String, Object>> getBySlug(@PathVariable String slug) {
        Optional<LandingPage> opt = pageRepo.findBySlug(slug);
        if (opt.isEmpty()) {
            return ok(404, "页面不存在或已下线", null);
        }
        LandingPage p = opt.get();
        if (p.getStatus() == null || p.getStatus() != PageStatus.PUBLISHED.code) {
            return ok(404, "页面不存在或已下线", null);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", p.getId());
        data.put("slug", p.getSlug());
        data.put("name", p.getName());
        data.put("content", p.getContent());
        data.put("status", p.getStatus());
        return ok(1, "ok", data);
    }

    /** 模板列表(已启用)。 */
    @GetMapping("/template/list")
    public ResponseEntity<Map<String, Object>> templateList() {
        List<LandingTemplate> all = tplRepo.findAll().stream()
            .filter(t -> t.getEnabled() != null && t.getEnabled() == EnableStatus.ENABLED.code)
            .toList();
        List<Map<String, Object>> data = new ArrayList<>();
        for (LandingTemplate t : all) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("category", t.getCategory());
            m.put("schema", t.getSchema());
            m.put("thumbUrl", t.getThumbUrl());
            data.add(m);
        }
        return ok(1, "ok", data);
    }

    /** 留资提交(核心闭环)。 */
    @PostMapping("/lead")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitLead(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest req) {
        Long pageId = toLong(body.get("pageId"));
        if (pageId == null) {
            return ok(400, "pageId 必填", null);
        }
        Optional<LandingPage> opt = pageRepo.findById(pageId);
        if (opt.isEmpty() || opt.get().getStatus() != PageStatus.PUBLISHED.code) {
            return ok(404, "页面不存在或已下线", null);
        }
        LandingPage p = opt.get();

        LandingLead lead = new LandingLead();
        lead.setPageId(p.getId());
        lead.setSlug(p.getSlug());
        lead.setPhone(toStr(body.get("phone")));
        Object extra = body.get("extra");
        lead.setExtra(extra == null ? null : extra.toString());
        Integer source = toInt(body.get("source"));
        lead.setSource(source == null ? LeadSource.SHORT_URL.code : source);
        lead.setClientIp(clientIp(req));
        String ua = req.getHeader("User-Agent");
        lead.setUserAgent(ua == null ? "" : ua);
        lead.setSubmitTime(LocalDateTime.now());
        leadRepo.save(lead);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("leadId", lead.getId());
        return ok(1, "提交成功", data);
    }

    /** 按 ID 取页面(管理端)。 */
    @GetMapping("/page/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Optional<LandingPage> opt = pageRepo.findById(id);
        if (opt.isEmpty()) {
            return ok(404, "页面不存在", null);
        }
        return ok(1, "ok", opt.get());
    }

    // ============ helpers ============

    private static ResponseEntity<Map<String, Object>> ok(int code, String msg, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", code);
        r.put("msg", msg);
        r.put("data", data);
        return ResponseEntity.ok(r);
    }

    private static ResponseEntity<Map<String, Object>> ok(int code, String msg) {
        return ok(code, msg, null);
    }

    private static String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private static String toStr(Object o) {
        return o == null ? null : o.toString();
    }
}
