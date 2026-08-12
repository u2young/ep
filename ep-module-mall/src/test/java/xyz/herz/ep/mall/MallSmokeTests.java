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
}
