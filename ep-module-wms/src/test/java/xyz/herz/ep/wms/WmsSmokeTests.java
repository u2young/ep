package xyz.herz.ep.wms;

import xyz.herz.ep.wms.entity.*;
import xyz.herz.ep.wms.enums.WmsDictEnums.*;
import xyz.herz.ep.wms.handler.*;
import xyz.herz.ep.wms.jpa.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

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

    private WmsStock findStock(Long locationId, String skuCode) {
        return stockRepo.findByLocationSku(locationId, skuCode).orElse(null);
    }
}
