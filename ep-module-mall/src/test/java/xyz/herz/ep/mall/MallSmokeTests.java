package xyz.herz.ep.mall;

import xyz.herz.ep.mall.entity.MallPayOrder;
import xyz.herz.ep.mall.entity.MallProductBrand;
import xyz.herz.ep.mall.entity.MallProductCategory;
import xyz.herz.ep.mall.entity.MallProductSku;
import xyz.herz.ep.mall.entity.MallProductSpu;
import xyz.herz.ep.mall.entity.MallTradeAfterSale;
import xyz.herz.ep.mall.entity.MallTradeOrder;
import xyz.herz.ep.mall.entity.MallTradeOrderItem;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.AfterSaleType;
import xyz.herz.ep.mall.enums.MallDictEnums.EnableStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.ListingStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.OrderStatus;
import xyz.herz.ep.mall.enums.MallDictEnums.PayStatus;
import xyz.herz.ep.mall.handler.MallAfterSaleAgreeHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleBuyerShipHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleCompleteHandler;
import xyz.herz.ep.mall.handler.MallAfterSaleSellerReceiveHandler;
import xyz.herz.ep.mall.handler.MallOrderCancelHandler;
import xyz.herz.ep.mall.handler.MallOrderConfirmHandler;
import xyz.herz.ep.mall.handler.MallOrderPayHandler;
import xyz.herz.ep.mall.handler.MallOrderShipHandler;
import xyz.herz.ep.mall.jpa.MallPayOrderRepository;
import xyz.herz.ep.mall.jpa.MallProductBrandRepository;
import xyz.herz.ep.mall.jpa.MallProductCategoryRepository;
import xyz.herz.ep.mall.jpa.MallProductSkuRepository;
import xyz.herz.ep.mall.jpa.MallProductSpuRepository;
import xyz.herz.ep.mall.jpa.MallTradeAfterSaleRepository;
import xyz.herz.ep.mall.jpa.MallTradeOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商城模块冒烟测试(覆盖商品 + 订单状态机 + 售后状态机):
 * <ol>
 *   <li>商品分类/品牌/SPU/SKU CRUD + 上下架</li>
 *   <li>订单全链路:创建 → 付款 → 发货 → 确认收货 → 已完成(并校验支付单联动生成)</li>
 *   <li>订单取消:待付款取消 / 待发货取消</li>
 *   <li>售后流程:申请 → 同意 → 买家发货 → 卖家收货 → 完成</li>
 * </ol>
 */
@SpringBootTest(classes = MallTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class MallSmokeTests {

    @Autowired MallProductCategoryRepository catRepo;
    @Autowired MallProductBrandRepository brandRepo;
    @Autowired MallProductSpuRepository spuRepo;
    @Autowired MallProductSkuRepository skuRepo;
    @Autowired MallTradeOrderRepository orderRepo;
    @Autowired MallTradeAfterSaleRepository afterSaleRepo;
    @Autowired MallPayOrderRepository payOrderRepo;

    @Autowired MallOrderPayHandler payHandler;
    @Autowired MallOrderShipHandler shipHandler;
    @Autowired MallOrderConfirmHandler confirmHandler;
    @Autowired MallOrderCancelHandler cancelHandler;
    @Autowired MallAfterSaleAgreeHandler agreeHandler;
    @Autowired MallAfterSaleBuyerShipHandler buyerShipHandler;
    @Autowired MallAfterSaleSellerReceiveHandler sellerReceiveHandler;
    @Autowired MallAfterSaleCompleteHandler completeHandler;

    /** ApplicationContext 反射取 BUTTON Handler Bean。 */
    @Autowired ApplicationContext applicationContext;

    // =================== 1. 商品 CRUD + 上下架 ===================

    @Test
    void product_category_brand_spu_sku_crud_and_listing() {
        MallProductCategory cat = new MallProductCategory();
        cat.setName("数码"); cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);

        MallProductBrand brand = new MallProductBrand();
        brand.setName("测试品牌"); brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);

        long nano = System.nanoTime();
        MallProductSpu spu = new MallProductSpu();
        spu.setName("测试手机"); spu.setCode("SPU-" + nano);
        spu.setCategory(cat); spu.setBrand(brand);
        spu.setPicUrl("https://example.com/p.png");
        spu.setDescription("商品详情文本");
        spu.setStatus(ListingStatus.DELISTED.code);  // 初始下架
        MallProductSku sku = new MallProductSku();
        sku.setCode("SKU-" + nano); sku.setSpecs("128G");
        sku.setPrice(new BigDecimal("5999.00"));
        sku.setStock(100);
        sku.setProduct(spu);
        spu.setSkus(new ArrayList<>(List.of(sku)));
        spuRepo.save(spu);

        Long spuId = spu.getId();
        Long skuId = spu.getSkus().get(0).getId();
        assertNotNull(spuId);
        assertNotNull(skuId);

        // SKU 持久化校验
        MallProductSku dbSku = skuRepo.findById(skuId).orElseThrow();
        assertEquals("128G", dbSku.getSpecs());
        assertEquals(0, new BigDecimal("5999.00").compareTo(dbSku.getPrice()));
        assertEquals(spuId, dbSku.getProduct().getId());

        // 上架
        MallProductSpu toList = spuRepo.findById(spuId).orElseThrow();
        toList.setStatus(ListingStatus.LISTED.code);
        spuRepo.save(toList);
        assertEquals(ListingStatus.LISTED.code,
            spuRepo.findById(spuId).orElseThrow().getStatus());

        // 下架
        toList.setStatus(ListingStatus.DELISTED.code);
        spuRepo.save(toList);
        assertEquals(ListingStatus.DELISTED.code,
            spuRepo.findById(spuId).orElseThrow().getStatus());
    }

    // =================== 2. 订单全链路 ===================

    @Test
    void order_full_chain_pay_ship_confirm() {
        MallProductSku sku = basicSku("CHAIN");
        MallTradeOrder order = newOrder("ORD-FULL-" + System.nanoTime(), sku);
        orderRepo.save(order);
        Long orderId = order.getId();

        // 付款 0 → 10
        String r = payHandler.exec(List.of(order), null, new String[]{ MallOrderPayHandler.CODE });
        assertTrue(r.contains("成功 1"), () -> "付款失败:" + r);
        MallTradeOrder paid = orderRepo.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.UNDELIVERED.code, paid.getStatus());
        assertNotNull(paid.getPayTime());

        // 支付单联动生成且已支付
        List<MallPayOrder> pays = payOrderRepo.findAll().stream()
            .filter(p -> orderId.equals(p.getOrderId())).toList();
        assertEquals(1, pays.size());
        assertEquals(PayStatus.PAID.code, pays.get(0).getStatus());
        assertEquals(0, order.getPayAmount().compareTo(pays.get(0).getAmount()));

        // 发货 10 → 20
        String r2 = shipHandler.exec(List.of(paid), null, new String[]{ MallOrderShipHandler.CODE });
        assertTrue(r2.contains("成功 1"), () -> "发货失败:" + r2);
        MallTradeOrder shipped = orderRepo.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.UNRECEIVED.code, shipped.getStatus());
        assertNotNull(shipped.getShipTime());

        // 确认收货 20 → 30
        String r3 = confirmHandler.exec(List.of(shipped), null, new String[]{ MallOrderConfirmHandler.CODE });
        assertTrue(r3.contains("成功 1"), () -> "确认收货失败:" + r3);
        MallTradeOrder done = orderRepo.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.COMPLETED.code, done.getStatus());
        assertNotNull(done.getConfirmTime());

        // 状态机守护:已完成订单再次发货应失败
        String r4 = shipHandler.exec(List.of(done), null, new String[]{ MallOrderShipHandler.CODE });
        assertTrue(r4.contains("失败 1"), () -> "已完成订单不应再发货:" + r4);
    }

    // =================== 3. 订单取消 ===================

    @Test
    void order_cancel_from_unpaid_and_un_delivered() {
        // 待付款 → 取消
        MallProductSku sku1 = basicSku("C1");
        MallTradeOrder o1 = newOrder("ORD-C1-" + System.nanoTime(), sku1);
        orderRepo.save(o1);
        String r1 = cancelHandler.exec(List.of(o1), null, new String[]{ MallOrderCancelHandler.CODE });
        assertTrue(r1.contains("成功 1"), () -> "取消失败:" + r1);
        assertEquals(OrderStatus.CANCELED.code,
            orderRepo.findById(o1.getId()).orElseThrow().getStatus());
        assertNotNull(orderRepo.findById(o1.getId()).orElseThrow().getCancelTime());

        // 待发货 → 取消(先付款再取消)
        MallProductSku sku2 = basicSku("C2");
        MallTradeOrder o2 = newOrder("ORD-C2-" + System.nanoTime(), sku2);
        orderRepo.save(o2);
        payHandler.exec(List.of(o2), null, new String[]{ MallOrderPayHandler.CODE });
        assertEquals(OrderStatus.UNDELIVERED.code,
            orderRepo.findById(o2.getId()).orElseThrow().getStatus());
        String r2 = cancelHandler.exec(List.of(o2), null, new String[]{ MallOrderCancelHandler.CODE });
        assertTrue(r2.contains("成功 1"), () -> "待发货取消失败:" + r2);
        assertEquals(OrderStatus.CANCELED.code,
            orderRepo.findById(o2.getId()).orElseThrow().getStatus());
    }

    // =================== 4. 售后流程 ===================

    @Test
    void aftersale_agree_ship_receive_complete() {
        MallProductSku sku = basicSku("AS");
        MallTradeOrder order = newOrder("ORD-AS-" + System.nanoTime(), sku);
        orderRepo.save(order);
        Long itemId = order.getItems().get(0).getId();

        MallTradeAfterSale as = new MallTradeAfterSale();
        as.setNo("AS-" + System.nanoTime());
        as.setOrderId(order.getId());
        as.setOrderItemId(itemId);
        as.setType(AfterSaleType.REFUND_ONLY.code);
        as.setStatus(AfterSaleStatus.APPLY.code);
        as.setAmount(new BigDecimal("5999.00"));
        as.setReason("质量问题");
        afterSaleRepo.save(as);
        Long asId = as.getId();

        // 同意 10 → 20
        String r1 = agreeHandler.exec(List.of(as), null, new String[]{ MallAfterSaleAgreeHandler.CODE });
        assertTrue(r1.contains("成功 1"), () -> "同意失败:" + r1);
        assertEquals(AfterSaleStatus.AGREED.code,
            afterSaleRepo.findById(asId).orElseThrow().getStatus());

        // 买家发货 20 → 30
        String r2 = buyerShipHandler.exec(List.of(as), null, new String[]{ MallAfterSaleBuyerShipHandler.CODE });
        assertTrue(r2.contains("成功 1"), () -> "买家发货失败:" + r2);
        assertEquals(AfterSaleStatus.BUYER_DELIVERY.code,
            afterSaleRepo.findById(asId).orElseThrow().getStatus());

        // 卖家收货 30 → 40
        String r3 = sellerReceiveHandler.exec(List.of(as), null, new String[]{ MallAfterSaleSellerReceiveHandler.CODE });
        assertTrue(r3.contains("成功 1"), () -> "卖家收货失败:" + r3);
        assertEquals(AfterSaleStatus.SELLER_RECEIVE.code,
            afterSaleRepo.findById(asId).orElseThrow().getStatus());

        // 完成退款 40 → 60
        String r4 = completeHandler.exec(List.of(as), null, new String[]{ MallAfterSaleCompleteHandler.CODE });
        assertTrue(r4.contains("成功 1"), () -> "完成退款失败:" + r4);
        assertEquals(AfterSaleStatus.SUCCESS.code,
            afterSaleRepo.findById(asId).orElseThrow().getStatus());
    }

    // =================== TR-3.1/3.2 @DragSort 拖拽排序(RED→GREEN) ===================

    @Test
    void mall_brand_drag_sort_annotation_and_order() throws Exception {
        // TR-3.2 注解断言: @Erupt.dragSort.field="sort"
        xyz.erupt.annotation.Erupt eruptAnn =
            MallProductBrand.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertNotNull(eruptAnn, "MallProductBrand 应有 @Erupt");
        assertEquals("sort", eruptAnn.dragSort().field(),
            "MallProductBrand 必须配置 @DragSort(field=\"sort\") 支持列表拖拽排序");

        // TR-3.2 字段完整性: sort 是 Integer,默认值 0
        java.lang.reflect.Field sortF = MallProductBrand.class.getDeclaredField("sort");
        sortF.setAccessible(true);
        assertEquals(Integer.class, sortF.getType(), "sort 字段类型必须是 Integer");
        MallProductBrand empty = new MallProductBrand();
        assertEquals(0, sortF.get(empty), "sort 默认值应为 0");

        // TR-3.1 排序查询: save 3 条(sort=100/10/50) → 按 sort ASC 顺序 10,50,100
        MallProductBrand b100 = new MallProductBrand();
        b100.setName("品牌-s100"); b100.setSort(100); b100.setStatus(EnableStatus.ENABLED.code);
        MallProductBrand b10 = new MallProductBrand();
        b10.setName("品牌-s10"); b10.setSort(10); b10.setStatus(EnableStatus.ENABLED.code);
        MallProductBrand b50 = new MallProductBrand();
        b50.setName("品牌-s50"); b50.setSort(50); b50.setStatus(EnableStatus.ENABLED.code);
        brandRepo.saveAll(List.of(b100, b10, b50));
        List<MallProductBrand> ordered = brandRepo.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
            .limit(3).toList();
        assertEquals("品牌-s10", ordered.get(0).getName(), "sort ASC 第一个应为 10");
        assertEquals("品牌-s50", ordered.get(1).getName(), "sort ASC 第二个应为 50");
        assertEquals("品牌-s100", ordered.get(2).getName(), "sort ASC 第三个应为 100");
    }

    // =================== TR-4.1 collapseActionButton + TR-4.2 @Power(copy) (RED→GREEN) ===================

    @Test
    void collapse_action_button_for_order_and_aftersale_and_copy_for_spu() {
        // TR-4.1 (RED): MallTradeOrder/MallTradeAfterSale 应有 @Layout(collapseActionButton=true)
        xyz.erupt.annotation.sub_erupt.Layout layoutOrder =
            MallTradeOrder.class.getAnnotation(xyz.erupt.annotation.Erupt.class).layout();
        assertTrue(layoutOrder.collapseActionButton(),
            "MallTradeOrder(4 行按钮) 应启用 @Layout(collapseActionButton=true) 折叠动作按钮");

        xyz.erupt.annotation.sub_erupt.Layout layoutAS =
            MallTradeAfterSale.class.getAnnotation(xyz.erupt.annotation.Erupt.class).layout();
        assertTrue(layoutAS.collapseActionButton(),
            "MallTradeAfterSale(5 行按钮) 应启用 @Layout(collapseActionButton=true) 折叠动作按钮");

        // TR-4.2 (RED): MallProductSpu 应启用 @Power(copy=true)
        assertTrue(
            MallProductSpu.class.getAnnotation(xyz.erupt.annotation.Erupt.class).power().copy(),
            "MallProductSpu 高频商品档案实体 应启用 @Power(copy=true) 一键复制行"
        );

        // TR-4.2 (RED): 后端复制行为验证(等价前端 Power.copy 的后端落地)
        // new 基础对象 + save → 再"复制"(new SPU 实例 + copy code/name/... + setId(null) + save)
        MallProductCategory cat = new MallProductCategory();
        cat.setName("copycat-" + System.nanoTime());
        cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        MallProductBrand brand = new MallProductBrand();
        brand.setName("copybrand-" + System.nanoTime());
        brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);

        MallProductSpu src = new MallProductSpu();
        src.setName("原商品"); src.setCode("COPY-SPU-" + System.nanoTime());
        src.setCategory(cat); src.setBrand(brand);
        src.setPicUrl("http://x/a.png"); src.setDescription("详情文本");
        src.setStatus(ListingStatus.LISTED.code);
        spuRepo.save(src);
        assertNotNull(src.getId());

        // 后端复制(等价 @Power copy 按钮前端执行后提交)
        MallProductSpu cp = new MallProductSpu();
        cp.setName(src.getName() + "-副本");
        cp.setCode(src.getCode() + "-CP");  // unique 约束
        cp.setCategory(src.getCategory());
        cp.setBrand(src.getBrand());
        cp.setPicUrl(src.getPicUrl());
        cp.setDescription(src.getDescription());
        cp.setStatus(src.getStatus());
        cp.setId(null);  // 关键: 清空 ID,强制新插入
        spuRepo.save(cp);

        // 验证复制结果
        assertNotNull(cp.getId(), "复制后必须生成新 ID");
        assertNotEquals(src.getId(), cp.getId(), "新 ID 不能等于源 ID");
        MallProductSpu cpDb = spuRepo.findById(cp.getId()).orElseThrow();
        assertEquals(src.getCategory().getId(), cpDb.getCategory().getId(),
            "复制 SPU 应保留 分类关联");
        assertEquals(src.getBrand().getId(), cpDb.getBrand().getId(),
            "复制 SPU 应保留 品牌关联");
        assertEquals(src.getPicUrl(), cpDb.getPicUrl(),
            "复制 SPU 应保留 主图");
        assertEquals(ListingStatus.LISTED.code, cpDb.getStatus(),
            "复制 SPU 应保留 上下架状态");
    }

    // =================== helpers ===================

    private MallProductSku basicSku(String seed) {
        MallProductCategory cat = new MallProductCategory();
        cat.setName("cat-" + seed); cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        MallProductBrand brand = new MallProductBrand();
        brand.setName("品牌-" + seed); brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);
        long nano = System.nanoTime();
        MallProductSpu spu = new MallProductSpu();
        spu.setName("商品-" + seed); spu.setCode("SPU-" + seed + nano);
        spu.setCategory(cat); spu.setBrand(brand);
        spu.setStatus(ListingStatus.LISTED.code);
        MallProductSku sku = new MallProductSku();
        sku.setCode("SKU-" + seed + nano); sku.setSpecs(seed);
        sku.setPrice(new BigDecimal("5999.00"));
        sku.setStock(1000);
        sku.setProduct(spu);
        spu.setSkus(new ArrayList<>(List.of(sku)));
        spuRepo.save(spu);
        return skuRepo.findById(spu.getSkus().get(0).getId()).orElseThrow();
    }

    private MallTradeOrder newOrder(String no, MallProductSku sku) {
        MallTradeOrder order = new MallTradeOrder();
        order.setNo(no); order.setUserId(1001L);
        order.setStatus(OrderStatus.UNPAID.code);
        order.setTotalAmount(new BigDecimal("5999.00"));
        order.setPayAmount(new BigDecimal("5999.00"));
        MallTradeOrderItem item = new MallTradeOrderItem();
        item.setSkuId(sku.getId());
        item.setSkuName(sku.getProduct().getName());
        item.setSkuPic(sku.getProduct().getPicUrl());
        item.setSkuPrice(sku.getPrice());
        item.setSkuSpec(sku.getSpecs());
        item.setQuantity(1);
        item.setLockStock(1);
        item.setOrder(order);
        order.setItems(new ArrayList<>(List.of(item)));
        return order;
    }

    // =================== TR-5B Mall BUTTON: SPU 自动批量生成 SKU (RED→GREEN) ===================

    @Test
    void mall_spu_button_autosku_and_boundary() throws Exception {
        // ===== (1) 注解断言: MallProductSpu 要有 2 BUTTON 辅助字段 + price 基准价字段 =====
        // skuCount
        java.lang.reflect.Field skuCountF;
        try {
            skuCountF = MallProductSpu.class.getDeclaredField("skuCount");
        } catch (NoSuchFieldException e) {
            fail("MallProductSpu 缺少 BUTTON 辅助字段: skuCount（生成几个 SKU）");
            return;
        }
        assertNotNull(skuCountF.getAnnotation(jakarta.persistence.Transient.class),
            "MallProductSpu.skuCount 必须 @Transient（仅 BUTTON 输入,不入库）");
        xyz.erupt.annotation.EruptField skuCountAnn =
            skuCountF.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(skuCountAnn, "skuCount 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.BUTTON, skuCountAnn.edit().type(),
            "skuCount 编辑 type 应为 BUTTON");

        // priceDeltaPercents
        java.lang.reflect.Field deltasF;
        try {
            deltasF = MallProductSpu.class.getDeclaredField("priceDeltaPercents");
        } catch (NoSuchFieldException e) {
            fail("MallProductSpu 缺少 BUTTON 辅助字段: priceDeltaPercents（逗号分隔的增减百分比,如 \"10,-10,0\"）");
            return;
        }
        assertNotNull(deltasF.getAnnotation(jakarta.persistence.Transient.class),
            "MallProductSpu.priceDeltaPercents 必须 @Transient");
        xyz.erupt.annotation.EruptField deltasAnn =
            deltasF.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(deltasAnn);
        assertEquals(xyz.erupt.annotation.sub_field.EditType.BUTTON, deltasAnn.edit().type(),
            "priceDeltaPercents 编辑 type 应为 BUTTON");

        // SPU 有 price 基准价字段(持久化,BigDecimal)
        java.lang.reflect.Field priceF = MallProductSpu.class.getDeclaredField("price");
        assertEquals(BigDecimal.class, priceF.getType(), "MallProductSpu 应有 BigDecimal price 基准价字段");

        // ===== (2) Handler 存在性 + exec 方法签名 =====
        Class<?> handlerCls;
        try {
            handlerCls = Class.forName("xyz.herz.ep.mall.handler.MallSpuAutoSkuButtonHandler");
        } catch (ClassNotFoundException e) {
            fail("缺少 Mall BUTTON Handler: xyz.herz.ep.mall.handler.MallSpuAutoSkuButtonHandler");
            return;
        }
        java.lang.reflect.Method exec;
        try {
            exec = handlerCls.getMethod("exec",
                Integer.class, String.class, MallProductSpu.class);
        } catch (NoSuchMethodException e) {
            fail("MallSpuAutoSkuButtonHandler 必须暴露 exec(Integer skuCount, String priceDeltaPercents, MallProductSpu spu) -> String");
            return;
        }
        Object handler = applicationContext.getBean(handlerCls);
        assertNotNull(handler);

        // ===== (3) TR-5B.1 正常路径: 基准价 100 / 3 SKU / deltas "10,-10,0" =====
        MallProductCategory cat = new MallProductCategory();
        cat.setName("AutoSKU-cat-" + System.nanoTime());
        cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        MallProductBrand brand = new MallProductBrand();
        brand.setName("AutoSKU-brand-" + System.nanoTime());
        brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);

        MallProductSpu spu = new MallProductSpu();
        spu.setName("自动生成SKU商品");
        spu.setCode("SPU-AUTOSKU-" + System.nanoTime());
        spu.setCategory(cat);
        spu.setBrand(brand);
        spu.setStatus(ListingStatus.LISTED.code);
        spu.setPrice(new BigDecimal("100"));      // 基准价 100
        spuRepo.save(spu);
        assertNotNull(spu.getId());

        String r = (String) exec.invoke(handler, 3, "10,-10,0", spu);
        assertTrue(r.contains("3"), () -> "exec 返回应含生成条数 3,实际:" + r);

        List<MallProductSku> skus = skuRepo.findByProductOrderByIdAsc(spu);
        assertEquals(3, skus.size(), "3 deltas -> 生成 3 SKU");
        // idx 0 -> delta +10% -> price = 100 × 110/100 = 110.00
        assertEquals(0, new BigDecimal("110.00").compareTo(skus.get(0).getPrice()),
            "SKU[0] delta=10% -> price=100×110%=110.00");
        assertEquals("10%", skus.get(0).getSpecs());
        assertTrue(skus.get(0).getCode().endsWith("-SKU-1"),
            "SKU code 应以 -SKU-1 结尾,实际=" + skus.get(0).getCode());

        // idx 1 -> delta -10% -> price = 90.00
        assertEquals(0, new BigDecimal("90.00").compareTo(skus.get(1).getPrice()),
            "SKU[1] delta=-10% -> price=100×90%=90.00");
        assertEquals("-10%", skus.get(1).getSpecs());
        assertTrue(skus.get(1).getCode().endsWith("-SKU-2"));

        // idx 2 -> delta 0% -> price = 100.00
        assertEquals(0, new BigDecimal("100.00").compareTo(skus.get(2).getPrice()),
            "SKU[2] delta=0% -> price=100.00");
        assertEquals("0%", skus.get(2).getSpecs());
        assertTrue(skus.get(2).getCode().endsWith("-SKU-3"));

        // stock 默认 0
        for (int i = 0; i < 3; i++) {
            assertEquals(Integer.valueOf(0), skus.get(i).getStock(),
                "新生成 SKU 初始库存应为 0");
        }

        // ===== (4) TR-5B.2 边界: spu.price == null 抛 IllegalStateException =====
        MallProductSpu spuNullPrice = new MallProductSpu();
        spuNullPrice.setName("空价商品");
        spuNullPrice.setCode("SPU-NULL-" + System.nanoTime());
        spuNullPrice.setCategory(cat);
        spuNullPrice.setBrand(brand);
        spuNullPrice.setStatus(ListingStatus.LISTED.code);
        spuNullPrice.setPrice(null);
        spuRepo.save(spuNullPrice);
        try {
            exec.invoke(handler, 2, "5,5", spuNullPrice);
            fail("spu.price=null 应抛 IllegalStateException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertNotNull(cause, "spu.price=null 必须抛异常");
            assertTrue(cause instanceof IllegalStateException,
                "spu.price=null 应抛 IllegalStateException,实际 cause="
                    + cause.getClass().getSimpleName() + ":" + cause.getMessage());
        }

        // ===== (5) 补充: skuCount <= 0 抛 IllegalArgumentException =====
        try {
            exec.invoke(handler, 0, "1,2,3", spu);
            fail("skuCount=0 应抛 IllegalArgumentException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof IllegalArgumentException,
                "skuCount=0 应抛 IAE, cause=" + (cause == null ? null : cause.getClass().getSimpleName()));
        }

        // ===== (6) 补充: deltas 数量 != skuCount 抛 IllegalArgumentException =====
        try {
            exec.invoke(handler, 3, "10,20", spu);
            fail("deltas 数目(2) != skuCount(3) 应抛 IAE");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof IllegalArgumentException,
                "deltas 数量与 skuCount 不一致应抛 IAE, cause="
                    + (cause == null ? null : cause.getClass().getSimpleName()));
        }
    }
}
