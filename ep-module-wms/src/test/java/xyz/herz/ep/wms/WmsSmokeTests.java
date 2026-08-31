package xyz.herz.ep.wms;

import xyz.herz.ep.wms.entity.*;
import xyz.herz.ep.wms.enums.WmsDictEnums.*;
import xyz.herz.ep.wms.handler.*;
import xyz.herz.ep.wms.jpa.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WMS P0 冒烟测试(覆盖入库三单 + 出库三单 + 四态库存):
 * <ol>
 *   <li>仓库/库区/库位三级 CRUD</li>
 *   <li>入库:ASN → 收货 → 上架 → 库存增加</li>
 *   <li>出库:出库通知 → 拣货 → 库存扣减</li>
 *   <li>四态库存校验:available/locked 正确</li>
 * </ol>
 */
@SpringBootTest(classes = WmsTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class WmsSmokeTests {

    @Autowired WmsWarehouseRepository whRepo;
    @Autowired WmsZoneRepository zoneRepo;
    @Autowired WmsLocationRepository locRepo;
    @Autowired WmsStockRepository stockRepo;
    @Autowired WmsStockMoveRepository moveRepo;
    @Autowired WmsAsnRepository asnRepo;
    @Autowired WmsAsnItemRepository asnItemRepo;
    @Autowired WmsShipmentNoticeRepository noticeRepo;
    @Autowired WmsShipmentItemRepository shipItemRepo;
    @Autowired WmsReceiptRepository receiptRepo;
    @Autowired WmsPutawayRepository putawayRepo;
    @Autowired WmsPickRepository pickRepo;
    @Autowired WmsStockMoveOrderRepository moveOrderRepo;
    @Autowired WmsStockCheckRepository checkRepo;

    @Autowired WmsAsnCloseHandler asnClose;
    @Autowired WmsReceiptCompleteHandler receiptComplete;
    @Autowired WmsPutawayCompleteHandler putawayComplete;
    @Autowired WmsPickCompleteHandler pickComplete;
    @Autowired WmsShipmentNoticeCloseHandler noticeClose;
    @Autowired WmsStockMoveCompleteHandler stockMoveComplete;
    @Autowired WmsStockCheckStartHandler stockCheckStart;
    @Autowired WmsStockCheckFinishHandler stockCheckFinish;

    /** 反射获取 BUTTON Handler Bean。 */
    @Autowired ApplicationContext applicationContext;

    // =================== 1. 仓库/库区/库位三级 CRUD ===================

    @Test
    void warehouse_zone_location_crud() {
        // 创建仓库
        WmsWarehouse wh = new WmsWarehouse();
        wh.setCode("WH-001");
        wh.setName("主仓库");
        wh.setAddress("上海市浦东新区");
        wh.setStatus(EnableStatus.ENABLED.code);
        whRepo.save(wh);
        assertNotNull(wh.getId());
        assertEquals("主仓库", whRepo.findByCode("WH-001").orElseThrow().getName());

        // 创建库区
        WmsZone zone = new WmsZone();
        zone.setWarehouse(wh);
        zone.setCode("Z-A");
        zone.setName("A 区");
        zone.setSort(1);
        zoneRepo.save(zone);
        assertEquals(1, zoneRepo.findByWarehouseId(wh.getId()).size());

        // 创建库位
        WmsLocation loc = new WmsLocation();
        loc.setZone(zone);
        loc.setCode("L-A-01");
        loc.setName("A-01");
        loc.setStatus(LocationStatus.IDLE.code);
        loc.setSort(1);
        locRepo.save(loc);
        assertEquals(1, locRepo.findByZoneId(zone.getId()).size());

        // 更新库位状态
        loc.setStatus(LocationStatus.HAS_STOCK.code);
        locRepo.save(loc);
        assertEquals(LocationStatus.HAS_STOCK.code,
            locRepo.findById(loc.getId()).orElseThrow().getStatus());

        // 删除库位
        locRepo.delete(loc);
        assertTrue(locRepo.findByZoneId(zone.getId()).isEmpty());

        // 删除库区级联检查(库位已删,库区可删)
        zoneRepo.delete(zone);
        assertTrue(zoneRepo.findByWarehouseId(wh.getId()).isEmpty());
    }

    // =================== 2. 入库: ASN → 收货 → 上架 → 库存增加 ===================

    @Test
    void inbound_asn_receipt_putaway_stock_increase() {
        WmsLocation loc = setupLocation();
        String sku = "SKU-IN-01";
        String skuName = "入库测试品";
        int qty = 50;

        // 2.1 创建 ASN
        WmsAsn asn = new WmsAsn();
        asn.setNo("ASN-001");
        asn.setWarehouse(loc.getZone().getWarehouse());
        asn.setSupplierName("测试供应商");
        asn.setStatus(AsnStatus.NEW.code);
        asn.setExpectedArrivalDate(LocalDate.now().plusDays(3));
        WmsAsnItem ai = new WmsAsnItem();
        ai.setAsn(asn);
        ai.setSkuCode(sku);
        ai.setSkuName(skuName);
        ai.setExpectedQty(qty);
        ai.setReceivedQty(0);
        asn.setItems(new ArrayList<>(List.of(ai)));
        asnRepo.save(asn);
        assertEquals(AsnStatus.NEW.code, asn.getStatus());

        // 2.2 创建收货单并完成
        WmsReceipt receipt = new WmsReceipt();
        receipt.setNo("RC-001");
        receipt.setAsnId(asn.getId());
        receipt.setWarehouse(loc.getZone().getWarehouse());
        receipt.setStatus(ReceiptStatus.NEW.code);
        WmsReceiptItem ri = new WmsReceiptItem();
        ri.setReceipt(receipt);
        ri.setSkuCode(sku);
        ri.setReceivedQty(qty);
        receipt.setItems(new ArrayList<>(List.of(ri)));
        receiptRepo.save(receipt);

        String r = receiptComplete.exec(List.of(receipt), null,
            new String[]{ WmsReceiptCompleteHandler.CODE_COMPLETE });
        assertTrue(r.contains("成功 1"), () -> "收货完成失败: " + r);

        // 验证收货单状态
        assertEquals(ReceiptStatus.COMPLETED.code,
            receiptRepo.findById(receipt.getId()).orElseThrow().getStatus());

        // 验证 ASN 回写
        WmsAsn asnAfter = asnRepo.findById(asn.getId()).orElseThrow();
        assertEquals(AsnStatus.RECEIVED.code, asnAfter.getStatus(),
            "ASN 全部收货后状态应为 RECEIVED(20)");
        WmsAsnItem aiAfter = asnItemRepo.findByAsnAndSku(asn.getId(), sku).orElseThrow();
        assertEquals(qty, aiAfter.getReceivedQty(), "ASN 明细 receivedQty 应回写");

        // 2.3 创建上架单并完成
        WmsPutaway putaway = new WmsPutaway();
        putaway.setNo("PA-001");
        putaway.setReceiptId(receipt.getId());
        putaway.setStatus(PutawayStatus.NEW.code);
        WmsPutawayItem pi = new WmsPutawayItem();
        pi.setPutaway(putaway);
        pi.setSkuCode(sku);
        pi.setLocationId(loc.getId());
        pi.setQty(qty);
        putaway.setItems(new ArrayList<>(List.of(pi)));
        putawayRepo.save(putaway);

        String r2 = putawayComplete.exec(List.of(putaway), null,
            new String[]{ WmsPutawayCompleteHandler.CODE_COMPLETE });
        assertTrue(r2.contains("成功 1"), () -> "上架完成失败: " + r2);

        // 验证上架单状态
        assertEquals(PutawayStatus.COMPLETED.code,
            putawayRepo.findById(putaway.getId()).orElseThrow().getStatus());

        // 验证库存增加
        WmsStock stock = findStock(loc.getId(), sku);
        assertNotNull(stock, "上架后应有库存记录");
        assertEquals(qty, stock.getAvailableQty(), "可用库存应 = 上架数量");
        assertEquals(0, stock.getLockedQty(), "锁定库存应为 0");
        assertEquals(0, stock.getInTransitQty(), "在途库存应为 0");
        assertEquals(0, stock.getFrozenQty(), "冻结库存应为 0");

        // 验证流水
        List<WmsStockMove> moves = moveRepo.findByBizType("PUTAWAY");
        assertFalse(moves.isEmpty(), "应有一条 PUTAWAY 流水");
        assertEquals(qty, moves.get(0).getQty());
        assertEquals(sku, moves.get(0).getSkuCode());
        assertEquals(loc.getId(), moves.get(0).getToLocationId());
        assertNull(moves.get(0).getFromLocationId());
    }

    // =================== 3. 出库: 出库通知 → 拣货 → 库存扣减 ===================

    @Test
    void outbound_notice_pick_stock_decrease() {
        WmsLocation loc = setupLocation();
        String sku = "SKU-OUT-01";
        String skuName = "出库测试品";
        int putawayQty = 50;
        int pickQty = 30;

        // 3.1 先入库备货(走完整 ASN→收货→上架 流程)
        inboundStock(loc, sku, skuName, putawayQty);
        WmsStock stockBefore = findStock(loc.getId(), sku);
        assertNotNull(stockBefore);
        assertEquals(putawayQty, stockBefore.getAvailableQty());

        // 3.2 创建出库通知
        WmsShipmentNotice notice = new WmsShipmentNotice();
        notice.setNo("SN-001");
        notice.setWarehouse(loc.getZone().getWarehouse());
        notice.setCustomerName("测试客户");
        notice.setStatus(ShipmentNoticeStatus.NEW.code);
        WmsShipmentItem si = new WmsShipmentItem();
        si.setNotice(notice);
        si.setSkuCode(sku);
        si.setSkuName(skuName);
        si.setExpectedQty(pickQty);
        si.setPickedQty(0);
        notice.setItems(new ArrayList<>(List.of(si)));
        noticeRepo.save(notice);
        assertEquals(ShipmentNoticeStatus.NEW.code, notice.getStatus());

        // 3.3 创建拣货单并完成
        WmsPick pick = new WmsPick();
        pick.setNo("PK-001");
        pick.setNoticeId(notice.getId());
        pick.setStatus(PickStatus.NEW.code);
        WmsPickItem pki = new WmsPickItem();
        pki.setPick(pick);
        pki.setSkuCode(sku);
        pki.setLocationId(loc.getId());
        pki.setPickQty(pickQty);
        pick.setItems(new ArrayList<>(List.of(pki)));
        pickRepo.save(pick);

        String r = pickComplete.exec(List.of(pick), null,
            new String[]{ WmsPickCompleteHandler.CODE_COMPLETE });
        assertTrue(r.contains("成功 1"), () -> "拣货完成失败: " + r);

        // 验证拣货单状态
        assertEquals(PickStatus.COMPLETED.code,
            pickRepo.findById(pick.getId()).orElseThrow().getStatus());

        // 验证库存扣减
        WmsStock stockAfter = findStock(loc.getId(), sku);
        assertNotNull(stockAfter);
        assertEquals(putawayQty - pickQty, stockAfter.getAvailableQty(),
            "可用库存应 = 入库 - 拣货 = " + (putawayQty - pickQty));
        assertEquals(0, stockAfter.getLockedQty(), "锁定库存应为 0");

        // 验证出库通知回写
        WmsShipmentNotice noticeAfter = noticeRepo.findById(notice.getId()).orElseThrow();
        assertEquals(ShipmentNoticeStatus.PICKED.code, noticeAfter.getStatus(),
            "全部拣货后通知状态应为 PICKED(20)");
        WmsShipmentItem siAfter = shipItemRepo.findByNoticeAndSku(notice.getId(), sku).orElseThrow();
        assertEquals(pickQty, siAfter.getPickedQty(), "通知明细 pickedQty 应回写");

        // 验证流水
        List<WmsStockMove> moves = moveRepo.findByBizType("PICK");
        assertFalse(moves.isEmpty(), "应有一条 PICK 流水");
        assertEquals(pickQty, moves.get(0).getQty());
        assertEquals(sku, moves.get(0).getSkuCode());
        assertEquals(loc.getId(), moves.get(0).getFromLocationId());
        assertNull(moves.get(0).getToLocationId());
    }

    // =================== 4. 四态库存校验 ===================

    @Test
    void four_state_stock_validation() {
        WmsLocation loc = setupLocation();
        String sku = "SKU-4STATE-01";
        int putawayQty = 100;
        int lockQty = 30;
        int pickQty = 30;

        // 4.1 入库 100
        inboundStock(loc, sku, "四态测试品", putawayQty);
        WmsStock s1 = findStock(loc.getId(), sku);
        assertNotNull(s1);
        assertEquals(100, s1.getAvailableQty());
        assertEquals(0, s1.getLockedQty());
        assertEquals(0, s1.getInTransitQty());
        assertEquals(0, s1.getFrozenQty());

        // 4.2 模拟分配:锁定 30(soft reservation,不扣 available)
        s1.setLockedQty(lockQty);
        stockRepo.save(s1);
        WmsStock s2 = findStock(loc.getId(), sku);
        assertEquals(100, s2.getAvailableQty(), "锁定不扣减可用");
        assertEquals(30, s2.getLockedQty());

        // 4.3 拣货 30:available -= 30, locked -= 30
        WmsShipmentNotice notice = new WmsShipmentNotice();
        notice.setNo("SN-4STATE");
        notice.setWarehouse(loc.getZone().getWarehouse());
        notice.setCustomerName("四态客户");
        notice.setStatus(ShipmentNoticeStatus.NEW.code);
        WmsShipmentItem si = new WmsShipmentItem();
        si.setNotice(notice);
        si.setSkuCode(sku);
        si.setExpectedQty(pickQty);
        si.setPickedQty(0);
        notice.setItems(new ArrayList<>(List.of(si)));
        noticeRepo.save(notice);

        WmsPick pick = new WmsPick();
        pick.setNo("PK-4STATE");
        pick.setNoticeId(notice.getId());
        pick.setStatus(PickStatus.NEW.code);
        WmsPickItem pki = new WmsPickItem();
        pki.setPick(pick);
        pki.setSkuCode(sku);
        pki.setLocationId(loc.getId());
        pki.setPickQty(pickQty);
        pick.setItems(new ArrayList<>(List.of(pki)));
        pickRepo.save(pick);

        String r = pickComplete.exec(List.of(pick), null,
            new String[]{ WmsPickCompleteHandler.CODE_COMPLETE });
        assertTrue(r.contains("成功 1"), () -> "拣货完成失败: " + r);

        // 4.4 四态校验
        WmsStock s3 = findStock(loc.getId(), sku);
        assertNotNull(s3);
        assertEquals(70, s3.getAvailableQty(), "可用 = 100 - 30 = 70");
        assertEquals(0, s3.getLockedQty(), "锁定 = max(0, 30-30) = 0");
        assertEquals(0, s3.getInTransitQty(), "在途不变");
        assertEquals(0, s3.getFrozenQty(), "冻结不变");

        // 4.5 额外校验:出库通知回写
        WmsShipmentNotice na = noticeRepo.findById(notice.getId()).orElseThrow();
        assertEquals(ShipmentNoticeStatus.PICKED.code, na.getStatus());
    }

    // =================== 额外: ASN 关闭 / 出库通知关闭 ===================

    @Test
    void asn_and_shipment_notice_close() {
        WmsLocation loc = setupLocation();
        WmsWarehouse wh = loc.getZone().getWarehouse();

        // ASN 关闭: NEW → CLOSED
        WmsAsn asn = new WmsAsn();
        asn.setNo("ASN-CLOSE-01");
        asn.setWarehouse(wh);
        asn.setStatus(AsnStatus.NEW.code);
        asn.setItems(new ArrayList<>());
        asnRepo.save(asn);

        String r1 = asnClose.exec(List.of(asn), null,
            new String[]{ WmsAsnCloseHandler.CODE_CLOSE });
        assertTrue(r1.contains("成功 1"), () -> "ASN 关闭失败: " + r1);
        assertEquals(AsnStatus.CLOSED.code,
            asnRepo.findById(asn.getId()).orElseThrow().getStatus());

        // 已关闭的 ASN 不能再关闭
        String r2 = asnClose.exec(List.of(asn), null,
            new String[]{ WmsAsnCloseHandler.CODE_CLOSE });
        assertTrue(r2.contains("失败 1"), "已关闭 ASN 应拒绝再次关闭");

        // 出库通知关闭: NEW → CLOSED
        WmsShipmentNotice notice = new WmsShipmentNotice();
        notice.setNo("SN-CLOSE-01");
        notice.setWarehouse(wh);
        notice.setStatus(ShipmentNoticeStatus.NEW.code);
        notice.setItems(new ArrayList<>());
        noticeRepo.save(notice);

        String r3 = noticeClose.exec(List.of(notice), null,
            new String[]{ WmsShipmentNoticeCloseHandler.CODE_CLOSE });
        assertTrue(r3.contains("成功 1"), () -> "通知关闭失败: " + r3);
        assertEquals(ShipmentNoticeStatus.CLOSED.code,
            noticeRepo.findById(notice.getId()).orElseThrow().getStatus());
    }

    // =================== 5. 移库作业:创建 → 完成 → 流水记录 ===================

    @Test
    void stock_move_create_complete_with_move_flow() {
        WmsLocation loc1 = setupLocation();
        WmsLocation loc2 = setupLocation();
        String sku = "SKU-MOVE-01";
        int moveQty = 20;

        // 先在 loc1 入库 50(便于后续校验库存基础,本测试主要校验流水)
        inboundStock(loc1, sku, "移库测试品", 50);
        WmsStock stockBefore = findStock(loc1.getId(), sku);
        assertNotNull(stockBefore);
        assertEquals(50, stockBefore.getAvailableQty());

        // 创建移库作业单:loc1 → loc2,qty=20
        WmsStockMoveOrder order = new WmsStockMoveOrder();
        order.setNo("MV-001");
        order.setWarehouse(loc1.getZone().getWarehouse());
        order.setStatus(StockMoveOrderStatus.NEW.code);
        WmsStockMoveOrderItem mi = new WmsStockMoveOrderItem();
        mi.setOrder(order);
        mi.setSkuCode(sku);
        mi.setFromLocationId(loc1.getId());
        mi.setToLocationId(loc2.getId());
        mi.setQty(moveQty);
        order.setItems(new ArrayList<>(List.of(mi)));
        moveOrderRepo.save(order);

        // 调用完成处理器:NEW → COMPLETED
        String r = stockMoveComplete.exec(List.of(order), null,
            new String[]{ WmsStockMoveCompleteHandler.CODE_COMPLETE });
        assertTrue(r.contains("成功 1"), () -> "移库完成失败: " + r);

        // 校验状态
        WmsStockMoveOrder orderAfter = moveOrderRepo.findById(order.getId()).orElseThrow();
        assertEquals(StockMoveOrderStatus.COMPLETED.code, orderAfter.getStatus(),
            "移库单完成后状态应为 COMPLETED(20)");

        // 校验流水:应有一条 MOVE 流水
        List<WmsStockMove> moves = moveRepo.findByBizType("MOVE");
        assertFalse(moves.isEmpty(), "应至少有一条 MOVE 流水");
        WmsStockMove m = moves.get(0);
        assertEquals(sku, m.getSkuCode());
        assertEquals(moveQty, m.getQty());
        assertEquals(loc1.getId(), m.getFromLocationId());
        assertEquals(loc2.getId(), m.getToLocationId());
    }

    // =================== 6. 盘点单:创建 → 开始 → 完成(有差异) ===================

    @Test
    void stock_check_create_start_finish_with_diff() {
        WmsLocation loc = setupLocation();
        String sku = "SKU-CHECK-01";
        int inboundQty = 50;
        int actualQty = 45; // 故意比账面少 5,产生差异

        // 先入库 50
        inboundStock(loc, sku, "盘点测试品", inboundQty);
        WmsStock stockBefore = findStock(loc.getId(), sku);
        assertNotNull(stockBefore);
        assertEquals(inboundQty, stockBefore.getAvailableQty());

        // 创建盘点单(单条明细,actualQty=45)
        WmsStockCheck check = new WmsStockCheck();
        check.setNo("SC-001");
        check.setWarehouse(loc.getZone().getWarehouse());
        check.setStatus(StockCheckStatus.NEW.code);
        WmsStockCheckItem ci = new WmsStockCheckItem();
        ci.setCheck(check);
        ci.setLocationId(loc.getId());
        ci.setSkuCode(sku);
        ci.setBookQty(0); // 开始盘点前为 0,START 时回写
        ci.setActualQty(actualQty);
        check.setItems(new ArrayList<>(List.of(ci)));
        checkRepo.save(check);

        // 调用开始盘点:NEW → CHECKING,回写 bookQty=50
        String r1 = stockCheckStart.exec(List.of(check), null,
            new String[]{ WmsStockCheckStartHandler.CODE_START });
        assertTrue(r1.contains("成功 1"), () -> "盘点开始失败: " + r1);

        WmsStockCheck checkAfterStart = checkRepo.findById(check.getId()).orElseThrow();
        assertEquals(StockCheckStatus.CHECKING.code, checkAfterStart.getStatus(),
            "盘点单开始后状态应为 CHECKING(10)");
        WmsStockCheckItem ciAfter = checkAfterStart.getItems().get(0);
        assertEquals(inboundQty, ciAfter.getBookQty(),
            "bookQty 应从 WmsStock 回写为 " + inboundQty);

        // 调用完成盘点:CHECKING → HAS_DIFF(30),diffQty = 45 - 50 = -5
        String r2 = stockCheckFinish.exec(List.of(checkAfterStart), null,
            new String[]{ WmsStockCheckFinishHandler.CODE_FINISH });
        assertTrue(r2.contains("成功 1"), () -> "盘点完成失败: " + r2);

        WmsStockCheck checkAfterFinish = checkRepo.findById(check.getId()).orElseThrow();
        assertEquals(StockCheckStatus.HAS_DIFF.code, checkAfterFinish.getStatus(),
            "有差异时状态应为 HAS_DIFF(30)");
        WmsStockCheckItem ciFinish = checkAfterFinish.getItems().get(0);
        assertEquals(actualQty - inboundQty, ciFinish.getDiffQty(),
            "diffQty 应 = actualQty - bookQty = " + (actualQty - inboundQty));
    }

    // =================== helpers ===================

    private WmsLocation setupLocation() {
        WmsWarehouse wh = new WmsWarehouse();
        wh.setCode("WH-" + System.nanoTime());
        wh.setName("测试仓");
        wh.setStatus(EnableStatus.ENABLED.code);
        whRepo.save(wh);

        WmsZone zone = new WmsZone();
        zone.setWarehouse(wh);
        zone.setCode("Z-" + System.nanoTime());
        zone.setName("测试区");
        zone.setSort(1);
        zoneRepo.save(zone);

        WmsLocation loc = new WmsLocation();
        loc.setZone(zone);
        loc.setCode("L-" + System.nanoTime());
        loc.setName("测试位");
        loc.setStatus(LocationStatus.IDLE.code);
        loc.setSort(1);
        locRepo.save(loc);
        return loc;
    }

    /** 走完整入库流程:ASN → 收货 → 上架,使库位产生 availableQty。 */
    private void inboundStock(WmsLocation loc, String sku, String skuName, int qty) {
        WmsWarehouse wh = loc.getZone().getWarehouse();

        // ASN
        WmsAsn asn = new WmsAsn();
        asn.setNo("ASN-" + System.nanoTime());
        asn.setWarehouse(wh);
        asn.setSupplierName("入库供应商");
        asn.setStatus(AsnStatus.NEW.code);
        asn.setExpectedArrivalDate(LocalDate.now());
        WmsAsnItem ai = new WmsAsnItem();
        ai.setAsn(asn);
        ai.setSkuCode(sku);
        ai.setSkuName(skuName);
        ai.setExpectedQty(qty);
        ai.setReceivedQty(0);
        asn.setItems(new ArrayList<>(List.of(ai)));
        asnRepo.save(asn);

        // 收货单 → 完成
        WmsReceipt receipt = new WmsReceipt();
        receipt.setNo("RC-" + System.nanoTime());
        receipt.setAsnId(asn.getId());
        receipt.setWarehouse(wh);
        receipt.setStatus(ReceiptStatus.NEW.code);
        WmsReceiptItem ri = new WmsReceiptItem();
        ri.setReceipt(receipt);
        ri.setSkuCode(sku);
        ri.setReceivedQty(qty);
        receipt.setItems(new ArrayList<>(List.of(ri)));
        receiptRepo.save(receipt);

        String r1 = receiptComplete.exec(List.of(receipt), null,
            new String[]{ WmsReceiptCompleteHandler.CODE_COMPLETE });
        assertTrue(r1.contains("成功 1"), () -> "入库-收货完成失败: " + r1);

        // 上架单 → 完成
        WmsPutaway putaway = new WmsPutaway();
        putaway.setNo("PA-" + System.nanoTime());
        putaway.setReceiptId(receipt.getId());
        putaway.setStatus(PutawayStatus.NEW.code);
        WmsPutawayItem pi = new WmsPutawayItem();
        pi.setPutaway(putaway);
        pi.setSkuCode(sku);
        pi.setLocationId(loc.getId());
        pi.setQty(qty);
        putaway.setItems(new ArrayList<>(List.of(pi)));
        putawayRepo.save(putaway);

        String r2 = putawayComplete.exec(List.of(putaway), null,
            new String[]{ WmsPutawayCompleteHandler.CODE_COMPLETE });
        assertTrue(r2.contains("成功 1"), () -> "入库-上架完成失败: " + r2);
    }

    // =================== TR-2.2 PROGRESS 盘点进度(RED→GREEN) ===================

    @Test
    void stock_check_progress_view_and_calc() throws Exception {
        // (1) 注解断言: 应有 checkProgress 虚拟字段 type=PROGRESS
        java.lang.reflect.Field f = WmsStockCheck.class.getDeclaredField("checkProgress");
        xyz.erupt.annotation.EruptField ann =
            f.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(ann, "WmsStockCheck 应有 checkProgress @EruptField 虚拟进度字段");
        assertEquals(xyz.erupt.annotation.sub_field.ViewType.PROGRESS,
            ann.views()[0].type(),
            "checkProgress 视图 type 应为 PROGRESS,用于盘点完成度条形展示");

        java.lang.reflect.Method getter =
            WmsStockCheck.class.getMethod("getCheckProgress");

        // (2) 比例 1: 明细 book=50,actual=25 → 盘点进度 50%
        WmsStockCheck c50 = new WmsStockCheck();
        WmsStockCheckItem i1 = new WmsStockCheckItem();
        i1.setBookQty(50);
        i1.setActualQty(25);
        i1.setCheck(c50);
        c50.setItems(new ArrayList<>(List.of(i1)));
        BigDecimal v50 = (BigDecimal) getter.invoke(c50);
        assertEquals(0, new BigDecimal("50.00").compareTo(v50),
            "actual=25 book=50 → 盘点进度应为 50.00%");

        // (3) 比例 2: book=50 actual=50 → 100%
        i1.setActualQty(50);
        BigDecimal v100 = (BigDecimal) getter.invoke(c50);
        assertEquals(0, new BigDecimal("100.00").compareTo(v100),
            "actual=book → 盘点进度应为 100%");

        // (4) 安全边界: items 为空 或 sum(bookQty)=0,返回 0
        WmsStockCheck empty = new WmsStockCheck();
        empty.setItems(new ArrayList<>());
        BigDecimal vEmpty = (BigDecimal) getter.invoke(empty);
        assertEquals(0, BigDecimal.ZERO.compareTo(vEmpty),
            "空明细盘点进度应安全返回 0");

        WmsStockCheck zeroBook = new WmsStockCheck();
        WmsStockCheckItem iz = new WmsStockCheckItem();
        iz.setBookQty(0);
        iz.setActualQty(0);
        iz.setCheck(zeroBook);
        zeroBook.setItems(new ArrayList<>(List.of(iz)));
        BigDecimal vZero = (BigDecimal) getter.invoke(zeroBook);
        assertEquals(0, BigDecimal.ZERO.compareTo(vZero),
            "sum(bookQty)=0 时进度应安全返回 0");
    }

    // =================== TR-4.2 @Power(copy=true) for WmsWarehouse + backend duplicate ===================

    @Test
    void wms_warehouse_power_copy_and_backend_duplicate() {
        // TR-4.2 (RED): WmsWarehouse 高频实体 应启用 @Power(copy=true)
        xyz.erupt.annotation.Erupt eruptAnn =
            WmsWarehouse.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertNotNull(eruptAnn, "WmsWarehouse 应有 @Erupt 注解");
        assertTrue(eruptAnn.power().copy(),
            "WmsWarehouse 高频仓库档案 应启用 @Power(copy=true) 一键复制行");

        // TR-4.2 (RED): 后端复制行为验证
        WmsWarehouse src = new WmsWarehouse();
        src.setName("源仓库");
        src.setCode("WH-COPY-" + System.nanoTime());
        src.setAddress("上海市浦东新区源地址");
        src.setStatus(EnableStatus.ENABLED.code);
        whRepo.save(src);
        assertNotNull(src.getId());

        WmsWarehouse cp = new WmsWarehouse();
        cp.setName(src.getName() + "-副本");
        cp.setCode(src.getCode() + "-CP");  // unique
        cp.setAddress(src.getAddress());
        cp.setStatus(src.getStatus());
        cp.setRemark(src.getRemark());
        cp.setId(null);
        whRepo.save(cp);

        assertNotNull(cp.getId(), "复制仓库必须生成新 ID");
        assertNotEquals(src.getId(), cp.getId());
        WmsWarehouse cpDb = whRepo.findById(cp.getId()).orElseThrow();
        assertEquals(src.getAddress(), cpDb.getAddress(), "复制仓库应保留地址");
        assertEquals(EnableStatus.ENABLED.code, cpDb.getStatus(), "复制仓库应保留启用状态");
    }

    private WmsStock findStock(Long locationId, String skuCode) {
        return stockRepo.findByLocationSku(locationId, skuCode).orElse(null);
    }

    // =================== TR-6B WMS BUTTON: 移库作业 推荐可用数量 (RED→GREEN) ===================

    @Test
    void wms_move_order_button_recommend_and_boundary() throws Exception {
        // ===== (1) 注解断言: WmsStockMoveOrder BUTTON 辅助字段 runRecommendQtyTrigger =====
        java.lang.reflect.Field trigF;
        try {
            trigF = WmsStockMoveOrder.class.getDeclaredField("runRecommendQtyTrigger");
        } catch (NoSuchFieldException e) {
            fail("WmsStockMoveOrder 缺少 BUTTON 辅助字段: runRecommendQtyTrigger（点按钮触发全单推荐移库数）");
            return;
        }
        assertNotNull(trigF.getAnnotation(jakarta.persistence.Transient.class),
            "runRecommendQtyTrigger 必须 @Transient");
        xyz.erupt.annotation.EruptField ann =
            trigF.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(ann, "runRecommendQtyTrigger 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.BUTTON, ann.edit().type(),
            "runRecommendQtyTrigger 编辑 type 应为 BUTTON（根据源库位实际可用库存 clamp 移库数）");

        // ===== (2) Handler 存在性 + exec 签名 =====
        Class<?> handlerCls;
        try {
            handlerCls = Class.forName("xyz.herz.ep.wms.handler.WmsMoveRecommendButtonHandler");
        } catch (ClassNotFoundException e) {
            fail("缺少 WMS BUTTON Handler: xyz.herz.ep.wms.handler.WmsMoveRecommendButtonHandler");
            return;
        }
        java.lang.reflect.Method exec;
        try {
            exec = handlerCls.getMethod("exec", WmsStockMoveOrder.class);
        } catch (NoSuchMethodException e) {
            fail("WmsMoveRecommendButtonHandler 必须暴露 exec(WmsStockMoveOrder order) -> String");
            return;
        }
        Object handler = applicationContext.getBean(handlerCls);
        assertNotNull(handler);

        // ===== (3) TR-6B.1 正常路径: 3 条明细 clamp =====
        WmsLocation locA = setupLocation();  // LOC-A
        WmsLocation locB = setupLocation();  // LOC-B
        // LOC-A 入库: SKU-A 100、SKU-B 20
        inboundStock(locA, "SKU-TR6B-A", "SKU-A 测试品", 100);
        inboundStock(locA, "SKU-TR6B-B", "SKU-B 测试品", 20);
        // LOC-B: 不入库,SKU-A 库存=0(空)

        WmsWarehouse wh = locA.getZone().getWarehouse();
        WmsStockMoveOrder order = new WmsStockMoveOrder();
        order.setNo("MOVE-TR6B-" + System.nanoTime());
        order.setWarehouse(wh);
        order.setStatus(StockMoveOrderStatus.NEW.code);

        WmsStockMoveOrderItem i1 = new WmsStockMoveOrderItem();
        i1.setSkuCode("SKU-TR6B-A"); i1.setFromLocationId(locA.getId());
        i1.setToLocationId(locB.getId()); i1.setQty(150);  // 请求 150,实际 100 → clamp 100

        WmsStockMoveOrderItem i2 = new WmsStockMoveOrderItem();
        i2.setSkuCode("SKU-TR6B-B"); i2.setFromLocationId(locA.getId());
        i2.setToLocationId(locB.getId()); i2.setQty(20);   // 请求 20,实际 20 → 20 exact

        WmsStockMoveOrderItem i3 = new WmsStockMoveOrderItem();
        i3.setSkuCode("SKU-TR6B-A"); i3.setFromLocationId(locB.getId());
        i3.setToLocationId(locA.getId()); i3.setQty(10);   // LOC-B SKU-A 空 → 0
        order.setItems(new ArrayList<>(List.of(i1, i2, i3)));
        moveOrderRepo.save(order);
        assertNotNull(order.getId());

        String r = (String) exec.invoke(handler, order);
        assertTrue(r.contains("3"), () -> "应处理 3 条明细,返回:" + r);

        // 重查 moveOrderRepo (含明细,因为 Cascade.ALL + orphan + save 回填 item id)
        WmsStockMoveOrder ordDb = moveOrderRepo.findById(order.getId()).orElseThrow();
        List<WmsStockMoveOrderItem> items = ordDb.getItems();
        assertEquals(3, items.size(), "3 明细");

        // 按 SKU 顺序断言(先按 fromLocationId + skuCode 排序避免顺序依赖)
        java.util.Map<String,Integer> byKey = new java.util.HashMap<>();
        for (WmsStockMoveOrderItem it : items) {
            byKey.put(it.getFromLocationId() + ":" + it.getSkuCode(), it.getQty());
        }
        assertEquals(Integer.valueOf(100), byKey.get(locA.getId() + ":SKU-TR6B-A"),
            "LOC-A SKU-A avail=100 req=150 → clamp 推荐 100");
        assertEquals(Integer.valueOf(20),  byKey.get(locA.getId() + ":SKU-TR6B-B"),
            "LOC-A SKU-B avail=20 req=20 → exact 推荐 20");
        assertEquals(Integer.valueOf(0),   byKey.get(locB.getId() + ":SKU-TR6B-A"),
            "LOC-B SKU-A avail=0 req=10 → clamp 推荐 0");

        // ===== (4) TR-6B.2 边界: fromLocationId == null → IAE =====
        // 说明: 实体 fromLocationId 列 nullable=false,无法先 save 再测;
        // Handler 在遍历 items 第一句就会校验 fromLocationId,所以直接传内存对象即可触发 IAE,
        // 不需要先落库 (save 是 Handler 最后一步,在异常前不会执行)。
        WmsStockMoveOrder bad = new WmsStockMoveOrder();
        bad.setNo("MOVE-BAD-" + System.nanoTime());
        bad.setWarehouse(wh);
        WmsStockMoveOrderItem badItem = new WmsStockMoveOrderItem();
        badItem.setSkuCode("X");
        badItem.setFromLocationId(null);   // 违反约束
        badItem.setToLocationId(9999L);
        badItem.setQty(10);
        bad.setItems(new ArrayList<>(List.of(badItem)));
        try {
            exec.invoke(handler, bad);
            fail("明细 fromLocationId=null 应抛 IllegalArgumentException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            assertTrue(cause instanceof IllegalArgumentException,
                "fromLocationId=null 应抛 IAE, cause=" + (cause == null ? null : cause.getClass().getSimpleName()));
        }
    }
}
