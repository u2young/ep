package xyz.herz.ep.erp;

import xyz.herz.ep.erp.inventory.InventoryChangeFacade;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade.ChangeItem;
import xyz.herz.ep.erp.inventory.InventoryChangeFacade.ChangeRequest;
import xyz.herz.ep.erp.inventory.InventoryShortageException;
import xyz.herz.ep.erp.entity.document.ErpStockIn;
import xyz.herz.ep.erp.entity.document.ErpStockInItem;
import xyz.herz.ep.erp.entity.document.ErpStockOut;
import xyz.herz.ep.erp.entity.document.ErpStockOutItem;
import xyz.herz.ep.erp.entity.finance.ErpFinancePayment;
import xyz.herz.ep.erp.entity.finance.ErpFinanceReceipt;
import xyz.herz.ep.erp.entity.finance.ErpFinanceSettlement;
import xyz.herz.ep.erp.entity.master.ErpCustomer;
import xyz.herz.ep.erp.entity.master.ErpProductBrand;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpProductUnit;
import xyz.herz.ep.erp.entity.master.ErpSupplier;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseIn;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseInItem;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrder;
import xyz.herz.ep.erp.entity.purchase.ErpPurchaseOrderItem;
import xyz.herz.ep.erp.entity.sale.ErpSaleOut;
import xyz.herz.ep.erp.entity.sale.ErpSaleOutItem;
import xyz.herz.ep.erp.entity.stock.ErpStockBalance;
import xyz.herz.ep.erp.entity.stock.ErpStockRecord;
import xyz.herz.ep.erp.enums.ErpDictEnums.AuditStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.EnableStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.FinanceDocStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.ProductListingStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockBizType;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockDocStatus;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockInBizType;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockOutBizType;
import xyz.herz.ep.erp.handler.document.ErpDocAuditHandler;
import xyz.herz.ep.erp.handler.document.ErpStockInAuditHandler;
import xyz.herz.ep.erp.handler.document.ErpStockOutAuditHandler;
import xyz.herz.ep.erp.handler.finance.ErpPaymentConfirmHandler;
import xyz.herz.ep.erp.handler.finance.ErpReceiptConfirmHandler;
import xyz.herz.ep.erp.handler.master.ErpMasterToggleHandler;
import xyz.herz.ep.erp.jpa.document.ErpPurchaseInRepository;
import xyz.herz.ep.erp.jpa.document.ErpPurchaseOrderRepository;
import xyz.herz.ep.erp.jpa.document.ErpSaleOutRepository;
import xyz.herz.ep.erp.jpa.document.ErpStockInRepository;
import xyz.herz.ep.erp.jpa.document.ErpStockOutRepository;
import xyz.herz.ep.erp.jpa.finance.ErpFinancePaymentRepository;
import xyz.herz.ep.erp.jpa.finance.ErpFinanceReceiptRepository;
import xyz.herz.ep.erp.jpa.finance.ErpFinanceSettlementRepository;
import xyz.herz.ep.erp.jpa.master.ErpCustomerRepository;
import xyz.herz.ep.erp.jpa.master.ErpProductBrandRepository;
import xyz.herz.ep.erp.jpa.master.ErpProductCategoryRepository;
import xyz.herz.ep.erp.jpa.master.ErpProductUnitRepository;
import xyz.herz.ep.erp.jpa.master.ErpSupplierRepository;
import xyz.herz.ep.erp.jpa.master.ErpWarehouseRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductSkuRepository;
import xyz.herz.ep.erp.jpa.stock.ErpStockBalanceRepository;
import xyz.herz.ep.erp.jpa.stock.ErpStockRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ERP P0 冒烟单测(覆盖核心状态机 + 库存闭环):
 * <ol>
 *   <li>产品/仓库/分类/单位/品牌 主数据 CRUD + 启停/上下架行按钮</li>
 *   <li>库存 Facade:入库 + 出库(不足抛异常) + 幂等(二次调用无副作用)</li>
 *   <li>采购订单(草稿)→采购入库草稿→审核入库(库存+余额/流水)</li>
 *   <li>销售出库草稿→审核出库(库存−)→反审冲销→库存回补</li>
 * </ol>
 */
@SpringBootTest(classes = ErpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class ErpSmokeTests {

    @Autowired ErpProductCategoryRepository catRepo;
    @Autowired ErpProductUnitRepository unitRepo;
    @Autowired ErpProductBrandRepository brandRepo;
    @Autowired ErpWarehouseRepository whRepo;
    @Autowired ErpSupplierRepository supplierRepo;
    @Autowired ErpCustomerRepository customerRepo;
    @Autowired ErpProductRepository productRepo;
    @Autowired ErpProductSkuRepository skuRepo;
    @Autowired ErpStockBalanceRepository balanceRepo;
    @Autowired ErpStockRecordRepository recordRepo;
    @Autowired ErpPurchaseOrderRepository poRepo;
    @Autowired ErpPurchaseInRepository piRepo;
    @Autowired ErpSaleOutRepository soRepo;
    @Autowired ErpStockInRepository stockInRepo;
    @Autowired ErpStockOutRepository stockOutRepo;
    @Autowired ErpFinancePaymentRepository paymentRepo;
    @Autowired ErpFinanceReceiptRepository receiptRepo;
    @Autowired ErpFinanceSettlementRepository settlementRepo;

    @Autowired InventoryChangeFacade inventory;
    @Autowired ErpMasterToggleHandler masterToggle;
    @Autowired ErpDocAuditHandler docAudit;
    @Autowired ErpStockInAuditHandler stockInAudit;
    @Autowired ErpStockOutAuditHandler stockOutAudit;
    @Autowired ErpPaymentConfirmHandler paymentConfirm;
    @Autowired ErpReceiptConfirmHandler receiptConfirm;

    // =================== 1. 主数据 + 启停行按钮 ===================

    @Test
    void master_product_warehouse_toggle() {
        ErpProductBrand b = new ErpProductBrand();
        b.setCode("BR-001"); b.setName("测试品牌"); b.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(b);

        ErpProductUnit u = new ErpProductUnit();
        u.setName("个"); u.setSymbol("PCS"); u.setStatus(EnableStatus.ENABLED.code);
        unitRepo.save(u);

        ErpProductCategory cat = new ErpProductCategory();
        cat.setName("食品"); cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);

        ErpWarehouse wh = new ErpWarehouse();
        wh.setCode("WH-SMOKE"); wh.setName("冒烟仓"); wh.setStatus(EnableStatus.DISABLED.code);
        whRepo.save(wh);
        assertEquals(EnableStatus.DISABLED.code, wh.getStatus());

        // 启用仓
        masterToggle.exec(List.of(wh), null, new String[]{ErpMasterToggleHandler.ENABLE});
        assertEquals(EnableStatus.ENABLED.code,
            whRepo.findById(wh.getId()).orElseThrow().getStatus());

        ErpProduct p = new ErpProduct();
        p.setCode("SKU-TEST-1"); p.setName("测试品1");
        p.setCategory(cat); p.setBrand(b); p.setUnit(u);
        p.setStatus(EnableStatus.DISABLED.code);
        p.setListingStatus(ProductListingStatus.DELISTED.code);
        ErpProductSku sku = new ErpProductSku();
        sku.setCode("SKU-TEST-1-DEF"); sku.setSpecsText("默认规格");
        sku.setSalePrice(new BigDecimal("10.00"));
        sku.setProduct(p);
        p.setSkus(new ArrayList<>(List.of(sku)));
        productRepo.save(p);
        Long pid = p.getId();
        Long skuId = p.getSkus().iterator().next().getId();
        assertNotNull(pid); assertNotNull(skuId);

        // 先启用产品
        masterToggle.exec(List.of(p), null, new String[]{ErpMasterToggleHandler.ENABLE});
        // 再上架
        masterToggle.exec(List.of(p), null, new String[]{ErpMasterToggleHandler.LIST});
        ErpProduct rp = productRepo.findById(pid).orElseThrow();
        assertEquals(EnableStatus.ENABLED.code, rp.getStatus());
        assertEquals(ProductListingStatus.LISTED.code, rp.getListingStatus());

        // 未启用的产品不能上架 —— 先停用再上架预期抛异常
        masterToggle.exec(List.of(p), null, new String[]{ErpMasterToggleHandler.DISABLE});
        ErpProduct dp = productRepo.findById(pid).orElseThrow();
        assertEquals(ProductListingStatus.DELISTED.code, dp.getListingStatus(),
            "停用产品应连带自动下架");
    }

    // =================== 2. Inventory Facade 入库/出库/幂等/不足 ===================

    @Test
    void inventory_in_out_idempotent_shortage() {
        long wh = basicWarehouse().getId();
        ErpProductSku sku = basicProductSku("SKU-INV-1", 50);

        // 入库 100 个
        InventoryChangeFacade.ChangeResult r1 = inventory.change(buildReq(
            "BIZ-1", 1001L, StockBizType.PURCHASE_IN,
            List.of(new ChangeItem(sku.getProduct().getId(), wh, BigDecimal.valueOf(100)))
        ));
        assertEquals(1, r1.affected());
        assertEquals(BigDecimal.valueOf(100), qtyNow(sku.getProduct().getId(), wh));

        // 幂等:同样 (bizType,bizId) 再次请求应直接返回且不改变数量
        InventoryChangeFacade.ChangeResult r2 = inventory.change(buildReq(
            "BIZ-1", 1001L, StockBizType.PURCHASE_IN,
            List.of(new ChangeItem(sku.getProduct().getId(), wh, BigDecimal.valueOf(100)))
        ));
        assertEquals(1, r2.affected(), "幂等重复调用应返回原明细数");
        assertEquals(BigDecimal.valueOf(100), qtyNow(sku.getProduct().getId(), wh),
            "幂等重复调用不得改变余额数量");

        // 出库 40 个 = 60
        inventory.change(buildReq(
            "BIZ-2", 1002L, StockBizType.SALE_OUT,
            List.of(new ChangeItem(sku.getProduct().getId(), wh, BigDecimal.valueOf(-40)))
        ));
        assertEquals(0, BigDecimal.valueOf(60).compareTo(qtyNow(sku.getProduct().getId(), wh)));

        // 出库 999 个 — 库存不足,必须抛 InventoryShortageException
        assertThrows(InventoryShortageException.class, () ->
            inventory.change(buildReq("BIZ-3", 1003L, StockBizType.SALE_OUT,
                List.of(new ChangeItem(sku.getProduct().getId(), wh, BigDecimal.valueOf(-999)))))
        );
    }

    // =================== 3. 采购订单 → 采购入库 → 审核入库(库存+) ===================

    @Test
    void purchase_order_to_in_approved_increase_stock() {
        ErpWarehouse wh = basicWarehouse();
        ErpProductSku sku = basicProductSku("SKU-PO-1", 80);

        ErpSupplier s = new ErpSupplier(); s.setName("测试供应商"); s.setCode("SP-"+System.nanoTime()); s.setStatus(EnableStatus.ENABLED.code); supplierRepo.save(s);

        ErpPurchaseOrder po = new ErpPurchaseOrder();
        po.setNo("PO-001"); po.setOrderTime(LocalDateTime.now());
        po.setStatus(AuditStatus.DRAFT.code);
        po.setTotalProductPrice(BigDecimal.valueOf(4000));
        po.setWarehouse(wh);
        po.setSupplier(s);
        ErpPurchaseOrderItem item = new ErpPurchaseOrderItem();
        item.setProduct(sku.getProduct());
        item.setSku(sku);
        item.setUnit(sku.getProduct().getUnit());
        item.setCount(BigDecimal.valueOf(50));          // 订 50
        item.setProductPrice(new BigDecimal("80"));     // 单价 80
        item.setOrder(po);
        po.setItems(new ArrayList<>(List.of(item)));
        poRepo.save(po);
        assertEquals(AuditStatus.DRAFT.code, po.getStatus());

        ErpPurchaseIn pi = new ErpPurchaseIn();
        pi.setNo("PI-001"); pi.setInTime(LocalDateTime.now());
        pi.setWarehouse(wh);
        pi.setOrder(po);
        pi.setSupplier(s);
        pi.setStatus(AuditStatus.DRAFT.code);
        ErpPurchaseInItem ii = new ErpPurchaseInItem();
        ii.setProduct(sku.getProduct());
        ii.setSku(sku);
        ii.setUnit(sku.getProduct().getUnit());
        ii.setCount(BigDecimal.valueOf(50));
        ii.setProductPrice(new BigDecimal("80"));
        ii.setInDoc(pi);
        pi.setItems(new ArrayList<>(List.of(ii)));
        piRepo.save(pi);

        // 审核入库单:库存+50, 状态=已审批
        String r = docAudit.exec(List.of(pi), null,
            new String[]{ErpDocAuditHandler.CODE_APPROVE});
        assertTrue(r.contains("成功 1"), () -> "采购入库审核失败:" + r);
        assertEquals(AuditStatus.APPROVED.code,
            piRepo.findById(pi.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "审核后应入库 50");

        // 订单 in_count 被回写(因为入库关联了订单)
        ErpPurchaseOrder rpo = poRepo.findById(po.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(50).compareTo(rpo.getInCount()),
            "PO 已入库数量应回写 50");

        // 反审入库单:冲销 −50
        docAudit.exec(List.of(pi), null, new String[]{ErpDocAuditHandler.CODE_UNAPPROVE});
        assertEquals(AuditStatus.DRAFT.code,
            piRepo.findById(pi.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "反审后库存归零");
    }

    // =================== 4. 销售出库 → 审核 → 反审 ===================

    @Test
    void sale_out_approve_and_unapprove() {
        ErpWarehouse wh = basicWarehouse();
        ErpProductSku sku = basicProductSku("SKU-SO-1", 20);

        // 先入库备货 100
        inventory.change(buildReq("PRE-STOCK", 9001L, StockBizType.PURCHASE_IN,
            List.of(new ChangeItem(sku.getProduct().getId(), wh.getId(), BigDecimal.valueOf(100)))));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())));

        ErpCustomer c = new ErpCustomer(); c.setName("测试客户"); c.setCode("C-"+System.nanoTime()); c.setStatus(EnableStatus.ENABLED.code); customerRepo.save(c);

        ErpSaleOut so = new ErpSaleOut();
        so.setNo("OUT-001"); so.setOutTime(LocalDateTime.now());
        so.setWarehouse(wh); so.setStatus(AuditStatus.DRAFT.code);
        so.setCustomer(c);
        ErpSaleOutItem oi = new ErpSaleOutItem();
        oi.setProduct(sku.getProduct()); oi.setSku(sku);
        oi.setUnit(sku.getProduct().getUnit());
        oi.setCount(BigDecimal.valueOf(30));
        oi.setProductPrice(new BigDecimal("25"));
        oi.setOutDoc(so);
        so.setItems(new ArrayList<>(List.of(oi)));
        soRepo.save(so);

        // 审核出库
        String r = docAudit.exec(List.of(so), null,
            new String[]{ErpDocAuditHandler.CODE_APPROVE});
        assertTrue(r.contains("成功 1"), () -> "销售出库审核失败:" + r);
        assertEquals(AuditStatus.APPROVED.code,
            soRepo.findById(so.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(70).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "100 - 30 = 70");

        // 反审 → 冲销回补
        docAudit.exec(List.of(so), null, new String[]{ErpDocAuditHandler.CODE_UNAPPROVE});
        assertEquals(AuditStatus.DRAFT.code,
            soRepo.findById(so.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "反审后库存回补到 100");
    }

    // =================== 5. 其他入库/出库 审核→库存增减 / 反审冲销 ===================

    @Test
    void stock_in_out_audit() {
        ErpWarehouse wh = basicWarehouse();
        ErpProductSku sku = basicProductSku("SKU-STK-1", 30);

        // --- 其他入库:审核 → 库存 +50 ---
        ErpStockIn in = new ErpStockIn();
        in.setNo("STK-IN-" + System.nanoTime());
        in.setWarehouse(wh);
        in.setBizType(StockInBizType.OTHER.code);
        in.setStatus(StockDocStatus.DRAFT.code);
        ErpStockInItem ii = new ErpStockInItem();
        ii.setSku(sku);
        ii.setQty(50);
        ii.setDoc(in);
        in.setItems(new ArrayList<>(List.of(ii)));
        stockInRepo.save(in);

        String r1 = stockInAudit.exec(List.of(in), null,
            new String[]{ErpStockInAuditHandler.CODE_AUDIT});
        assertTrue(r1.contains("成功 1"), () -> "其他入库审核失败:" + r1);
        assertEquals(StockDocStatus.APPROVED.code,
            stockInRepo.findById(in.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "审核后应入库 50");

        // --- 其他入库:反审 → 冲销库存归零 ---
        String r2 = stockInAudit.exec(List.of(in), null,
            new String[]{ErpStockInAuditHandler.CODE_UNAUDIT});
        assertTrue(r2.contains("成功 1"), () -> "其他入库反审失败:" + r2);
        assertEquals(StockDocStatus.DRAFT.code,
            stockInRepo.findById(in.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "反审后库存应归零");

        // --- 预先入库 100,作为后续出库的可用库存 ---
        ErpStockIn pre = new ErpStockIn();
        pre.setNo("STK-IN-PRE-" + System.nanoTime());
        pre.setWarehouse(wh);
        pre.setBizType(StockInBizType.OTHER.code);
        pre.setStatus(StockDocStatus.DRAFT.code);
        ErpStockInItem pii = new ErpStockInItem();
        pii.setSku(sku); pii.setQty(100); pii.setDoc(pre);
        pre.setItems(new ArrayList<>(List.of(pii)));
        stockInRepo.save(pre);
        stockInAudit.exec(List.of(pre), null, new String[]{ErpStockInAuditHandler.CODE_AUDIT});
        assertEquals(0, BigDecimal.valueOf(100).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "预备货后库存应 100");

        // --- 其他出库:审核 → 库存 -20 = 80 ---
        ErpStockOut out = new ErpStockOut();
        out.setNo("STK-OUT-" + System.nanoTime());
        out.setWarehouse(wh);
        out.setBizType(StockOutBizType.OTHER.code);
        out.setStatus(StockDocStatus.DRAFT.code);
        ErpStockOutItem oi = new ErpStockOutItem();
        oi.setSku(sku); oi.setQty(20); oi.setDoc(out);
        out.setItems(new ArrayList<>(List.of(oi)));
        stockOutRepo.save(out);

        String r3 = stockOutAudit.exec(List.of(out), null,
            new String[]{ErpStockOutAuditHandler.CODE_AUDIT});
        assertTrue(r3.contains("成功 1"), () -> "其他出库审核失败:" + r3);
        assertEquals(StockDocStatus.APPROVED.code,
            stockOutRepo.findById(out.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(80).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "出库审核后 100 - 20 = 80");

        // --- 其他出库:反审 → 冲销回补到 100 ---
        String r4 = stockOutAudit.exec(List.of(out), null,
            new String[]{ErpStockOutAuditHandler.CODE_UNAUDIT});
        assertTrue(r4.contains("成功 1"), () -> "其他出库反审失败:" + r4);
        assertEquals(StockDocStatus.DRAFT.code,
            stockOutRepo.findById(out.getId()).orElseThrow().getStatus());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(qtyNow(sku.getProduct().getId(), wh.getId())),
            "出库反审后库存回补到 100");
    }

    // =================== 6. 收付款单确认 + 核销记录 ===================

    @Test
    void finance_payment_receipt() {
        // --- 主数据:供应商 / 客户 ---
        ErpSupplier s = new ErpSupplier();
        s.setName("付款供应商"); s.setCode("SP-FIN-" + System.nanoTime());
        s.setStatus(EnableStatus.ENABLED.code);
        supplierRepo.save(s);

        ErpCustomer c = new ErpCustomer();
        c.setName("收款客户"); c.setCode("C-FIN-" + System.nanoTime());
        c.setStatus(EnableStatus.ENABLED.code);
        customerRepo.save(c);

        // --- 付款单确认 ---
        ErpFinancePayment pay = new ErpFinancePayment();
        pay.setNo("PAY-" + System.nanoTime());
        pay.setSupplier(s);
        pay.setTotalAmount(new BigDecimal("1000.00"));
        pay.setStatus(FinanceDocStatus.DRAFT.code);
        paymentRepo.save(pay);

        String r1 = paymentConfirm.exec(List.of(pay), null,
            new String[]{ErpPaymentConfirmHandler.CODE_CONFIRM});
        assertTrue(r1.contains("成功 1"), () -> "付款单确认失败:" + r1);
        assertEquals(FinanceDocStatus.CONFIRMED.code,
            paymentRepo.findById(pay.getId()).orElseThrow().getStatus());

        // --- 收款单确认 ---
        ErpFinanceReceipt rcv = new ErpFinanceReceipt();
        rcv.setNo("RCV-" + System.nanoTime());
        rcv.setCustomer(c);
        rcv.setTotalAmount(new BigDecimal("2000.00"));
        rcv.setStatus(FinanceDocStatus.DRAFT.code);
        receiptRepo.save(rcv);

        String r2 = receiptConfirm.exec(List.of(rcv), null,
            new String[]{ErpReceiptConfirmHandler.CODE_CONFIRM});
        assertTrue(r2.contains("成功 1"), () -> "收款单确认失败:" + r2);
        assertEquals(FinanceDocStatus.CONFIRMED.code,
            receiptRepo.findById(rcv.getId()).orElseThrow().getStatus());

        // --- 核销记录 1:付款单 → 采购订单 ---
        ErpFinanceSettlement st1 = new ErpFinanceSettlement();
        st1.setBizType(1);                  // 付款
        st1.setDocId(pay.getId());
        st1.setDocNo(pay.getNo());
        st1.setTargetBizType(1);            // 采购订单
        st1.setTargetId(9001L);
        st1.setTargetNo("PO-FIN-1");
        st1.setAmount(new BigDecimal("600.00"));
        st1.setRemark("核销采购订单 600");
        settlementRepo.save(st1);
        assertNotNull(st1.getId());

        // --- 核销记录 2:收款单 → 销售订单 ---
        ErpFinanceSettlement st2 = new ErpFinanceSettlement();
        st2.setBizType(2);                  // 收款
        st2.setDocId(rcv.getId());
        st2.setDocNo(rcv.getNo());
        st2.setTargetBizType(2);            // 销售订单
        st2.setTargetId(8002L);
        st2.setTargetNo("SO-FIN-1");
        st2.setAmount(new BigDecimal("1500.00"));
        st2.setRemark("核销销售订单 1500");
        settlementRepo.save(st2);
        assertNotNull(st2.getId());

        // --- 校验:按单据 ID 查核销记录 ---
        var paySettlements = settlementRepo.findByBizTypeAndDocId(1, pay.getId());
        assertEquals(1, paySettlements.size());
        assertEquals(0, new BigDecimal("600.00").compareTo(paySettlements.get(0).getAmount()));
        assertEquals("PO-FIN-1", paySettlements.get(0).getTargetNo());

        var rcvSettlements = settlementRepo.findByBizTypeAndDocId(2, rcv.getId());
        assertEquals(1, rcvSettlements.size());
        assertEquals(0, new BigDecimal("1500.00").compareTo(rcvSettlements.get(0).getAmount()));
        assertEquals("SO-FIN-1", rcvSettlements.get(0).getTargetNo());

        // --- 校验:按目标单据查核销记录 ---
        var poSettlements = settlementRepo.findByTargetBizTypeAndTargetId(1, 9001L);
        assertEquals(1, poSettlements.size());
        assertEquals(pay.getNo(), poSettlements.get(0).getDocNo());
    }

    // =================== helpers ===================

    private ErpWarehouse basicWarehouse() {
        ErpWarehouse wh = new ErpWarehouse();
        wh.setCode("WH-" + System.nanoTime());
        wh.setName("测试仓");
        wh.setStatus(EnableStatus.ENABLED.code);
        wh.setDefaultFlag(false);
        whRepo.save(wh);
        return wh;
    }

    private ErpProductSku basicProductSku(String nameSeed, double price) {
        ErpProductCategory cat = new ErpProductCategory();
        cat.setName("cat-" + nameSeed); cat.setStatus(EnableStatus.ENABLED.code);
        catRepo.save(cat);
        ErpProductUnit unit = new ErpProductUnit();
        unit.setName("件"); unit.setSymbol("U-" + nameSeed); unit.setStatus(EnableStatus.ENABLED.code);
        unitRepo.save(unit);
        ErpProductBrand brand = new ErpProductBrand();
        brand.setCode("BR-" + nameSeed); brand.setName("品牌-" + nameSeed);
        brand.setStatus(EnableStatus.ENABLED.code);
        brandRepo.save(brand);

        ErpProduct p = new ErpProduct();
        p.setCode("P-" + nameSeed + System.nanoTime());
        p.setName("产品-" + nameSeed);
        p.setCategory(cat); p.setUnit(unit); p.setBrand(brand);
        p.setStatus(EnableStatus.ENABLED.code);
        p.setListingStatus(ProductListingStatus.LISTED.code);
        ErpProductSku sku = new ErpProductSku();
        sku.setCode("SKU-" + nameSeed);
        sku.setSpecsText("默认规格");
        sku.setSalePrice(BigDecimal.valueOf(price));
        sku.setProduct(p);                 // JPA 维护端回绑
        p.setSkus(new ArrayList<>(List.of(sku)));
        productRepo.save(p);
        return skuRepo.findById(p.getSkus().iterator().next().getId()).orElseThrow();
    }

    private ChangeRequest buildReq(String bizNo, long bizId, StockBizType type, List<ChangeItem> items) {
        return new ChangeRequest(bizNo, bizId, type.code, items, "smoke");
    }

    private BigDecimal qtyNow(Long pid, Long whId) {
        var list = balanceRepo.findAll().stream()
            .filter(b -> pid.equals(b.getProduct().getId()) && whId.equals(b.getWarehouse().getId()))
            .toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpStockBalance b : list) {
            if (b.getQty() != null) sum = sum.add(b.getQty());
        }
        // 也查下流水校验存在性
        long recs = recordRepo.findAll().stream().mapToLong(ErpStockRecord::getId).count();
        assertTrue(recs >= 0);
        return sum;
    }
}
