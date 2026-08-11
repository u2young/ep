package xyz.herz.ep.erp.core;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.common.inventory.InventoryChangeFacade;
import xyz.herz.ep.common.inventory.InventoryShortageException;
import xyz.herz.ep.erp.entity.master.ErpProductCategory;
import xyz.herz.ep.erp.entity.master.ErpWarehouse;
import xyz.herz.ep.erp.entity.product.ErpProduct;
import xyz.herz.ep.erp.entity.product.ErpProductSku;
import xyz.herz.ep.erp.entity.stock.ErpStockBalance;
import xyz.herz.ep.erp.entity.stock.ErpStockRecord;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockBizType;
import xyz.herz.ep.erp.jpa.master.ErpWarehouseRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductRepository;
import xyz.herz.ep.erp.jpa.product.ErpProductSkuRepository;
import xyz.herz.ep.erp.jpa.stock.ErpStockBalanceRepository;
import xyz.herz.ep.erp.jpa.stock.ErpStockRecordRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ERP 中台:库存变更统一入口。
 * <p>提供 {@link InventoryChangeFacade} 的默认实现(未来 WMS 确认库位后也通过它回写 ERP),
 * 以及 ERP 单据内部要用的「批次维度」包装类和辅助方法。
 */
@Service
public class ErpStockService implements InventoryChangeFacade {

    private final ErpStockBalanceRepository balanceRepo;
    private final ErpStockRecordRepository recordRepo;
    private final ErpProductRepository productRepo;
    private final ErpProductSkuRepository skuRepo;
    private final ErpWarehouseRepository whRepo;

    public ErpStockService(ErpStockBalanceRepository balanceRepo,
                           ErpStockRecordRepository recordRepo,
                           ErpProductRepository productRepo,
                           ErpProductSkuRepository skuRepo,
                           ErpWarehouseRepository whRepo) {
        this.balanceRepo = balanceRepo;
        this.recordRepo = recordRepo;
        this.productRepo = productRepo;
        this.skuRepo = skuRepo;
        this.whRepo = whRepo;
    }

    // ================ Facade 接口:批次="-",无 SKU 则回退到 product 自带第一条 SKU ================

    @Override
    @Transactional
    public ChangeResult change(ChangeRequest req) {
        StockBizType biz = StockBizType.of(req.bizType());

        // 幂等:相同 (bizType, bizId) 已写过直接返回,不重复改
        if (recordRepo.countByBiz(req.bizType(), req.bizId()) > 0) {
            List<ErpStockRecord> existing = recordRepo.findByBizTypeAndBizIdOrderByIdAsc(req.bizType(), req.bizId());
            return new ChangeResult(existing.size(),
                existing.isEmpty() ? 0L : existing.get(0).getId());
        }

        int affected = 0;
        long firstId = 0L;
        for (ChangeItem it : req.items()) {
            // 简化 Facade 约定:如果外部没传 skuId,就找产品的第一条 SKU(保证最小兼容)
            Long skuId = resolveSkuId(it.productId(), null);
            BigDecimal qty = it.qty();

            // 方向校验:入库必须>0;出库必须<0;其他出入库由 biz.inbound() 约束
            if (biz.inbound() && qty.signum() <= 0) {
                throw new IllegalArgumentException("入库业务 " + biz.label + " 数量必须 > 0");
            }
            if (!biz.inbound() && qty.signum() >= 0) {
                throw new IllegalArgumentException("出库业务 " + biz.label + " 数量必须 < 0");
            }

            ErpStockBalance bal = lockOrCreateBalance(it.productId(), skuId, it.warehouseId(), DEFAULT_BATCH);
            BigDecimal newQty = bal.getQty().add(qty);
            if (newQty.signum() < 0) {
                throw new InventoryShortageException(it.productId(), it.warehouseId(), bal.getQty(), qty.negate());
            }
            bal.setQty(newQty);
            bal.setCategory(lookupCategory(it.productId(), bal.getCategory()));
            balanceRepo.save(bal);

            ErpStockRecord rec = buildRecord(biz, req, it, skuId, DEFAULT_BATCH, newQty);
            recordRepo.save(rec);
            if (firstId == 0L) firstId = rec.getId();
            affected++;
        }
        return new ChangeResult(affected, firstId);
    }

    // ================ ERP 单据用的 SKU+批次 增强入口(只在本模块内部直接用) ================

    public record DetailedItem(
            Long productId,
            Long skuId,
            Long warehouseId,
            String batchNo,         // null="-"
            BigDecimal qty,
            BigDecimal unitCost      // 单位成本(可选,null 用旧值)
    ) { }

    /** 详细 SKU+批次 变更(供采购入库/销售出库审核调用,与 Facade 不同在于有 SKU 和 batch)。 */
    @Transactional
    public ChangeResult changeDetailed(ChangeRequest req, List<DetailedItem> detail) {
        StockBizType biz = StockBizType.of(req.bizType());
        if (recordRepo.countByBiz(req.bizType(), req.bizId()) > 0) {
            List<ErpStockRecord> ex = recordRepo.findByBizTypeAndBizIdOrderByIdAsc(req.bizType(), req.bizId());
            return new ChangeResult(ex.size(), ex.isEmpty() ? 0L : ex.get(0).getId());
        }

        int affected = 0; long firstId = 0L;
        for (DetailedItem it : detail) {
            Long skuId = resolveSkuId(it.productId(), it.skuId());
            String batchNo = it.batchNo() == null ? DEFAULT_BATCH : it.batchNo();

            ErpStockBalance bal = lockOrCreateBalance(it.productId(), skuId, it.warehouseId(), batchNo);
            BigDecimal newQty = bal.getQty().add(it.qty());
            if (newQty.signum() < 0) {
                throw new InventoryShortageException(it.productId(), it.warehouseId(), bal.getQty(), it.qty().negate());
            }
            bal.setQty(newQty);
            bal.setCategory(lookupCategory(it.productId(), bal.getCategory()));
            // 加权平均:只在入库(数量增加方向)时尝试更新 avgCost
            if (it.qty().signum() > 0 && it.unitCost() != null && it.unitCost().signum() > 0) {
                BigDecimal oldTotal = bal.getQty().subtract(it.qty()).multiply(bal.getAvgCost());
                BigDecimal addTotal = it.qty().multiply(it.unitCost());
                BigDecimal newTotal = oldTotal.add(addTotal);
                if (newQty.signum() > 0) {
                    BigDecimal avg = newTotal.divide(newQty, 6, java.math.RoundingMode.HALF_UP);
                    bal.setAvgCost(avg);
                }
            }
            balanceRepo.save(bal);

            ErpStockRecord rec = buildRecord(biz, req,
                new ChangeItem(it.productId(), it.warehouseId(), it.qty()),
                skuId, batchNo, newQty);
            recordRepo.save(rec);
            if (firstId == 0L) firstId = rec.getId();
            affected++;
        }
        return new ChangeResult(affected, firstId);
    }

    /** 辅助:反向冲销(反审场景,bizType 保持不变,明细数量反转后写新流水)。 */
    @Transactional
    public ChangeResult reverse(ChangeRequest req, List<DetailedItem> originalItems, String reverseReason) {
        List<DetailedItem> negated = originalItems.stream()
            .map(d -> new DetailedItem(d.productId(), d.skuId(), d.warehouseId(), d.batchNo(),
                d.qty().negate(), null))
            .toList();
        String remark = req.remark() == null ? "反审冲销" : req.remark() + " / 反审冲销";
        if (reverseReason != null) remark = remark + ": " + reverseReason;
        ChangeRequest reversed = new ChangeRequest(req.bizNo(), req.bizId(), req.bizType(), req.items(), remark);
        return changeDetailed(reversed, negated);
    }

    // ================ 内部辅助 ================

    public static final String DEFAULT_BATCH = "-";

    private Long resolveSkuId(Long productId, Long skuIdOrNull) {
        if (skuIdOrNull != null) return skuIdOrNull;
        ErpProduct p = productRepo.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("产品不存在: " + productId));
        if (p.getSkus() != null && !p.getSkus().isEmpty()) return p.getSkus().get(0).getId();
        throw new IllegalStateException("产品尚未创建 SKU: " + p.getCode());
    }

    /** 先查再建:如果余额行不存在,在同一事务内插入一行,后续 PESSIMISTIC_WRITE 会自然锁。 */
    private ErpStockBalance lockOrCreateBalance(Long productId, Long skuId, Long whId, String batchNo) {
        String bn = batchNo == null ? DEFAULT_BATCH : batchNo;
        Optional<ErpStockBalance> opt = balanceRepo.lockByKey(productId, skuId, whId, bn);
        if (opt.isPresent()) return opt.get();

        ErpProduct p = productRepo.findById(productId).orElseThrow();
        ErpProductSku s = skuRepo.findById(skuId).orElseThrow();
        ErpWarehouse w = whRepo.findById(whId).orElseThrow();
        ErpStockBalance bal = new ErpStockBalance();
        bal.setProduct(p);
        bal.setSku(s);
        bal.setWarehouse(w);
        bal.setBatchNo(bn);
        bal.setCategory(p.getCategory());
        bal.setQty(BigDecimal.ZERO);
        bal.setLockedQty(BigDecimal.ZERO);
        bal.setInTransitQty(BigDecimal.ZERO);
        bal.setAvgCost(p.getPurchasePrice() == null ? BigDecimal.ZERO : p.getPurchasePrice());
        balanceRepo.save(bal);
        // 保存后立即加锁 —— 直接从当前对象返回,事务内不会丢
        return balanceRepo.lockByKey(productId, skuId, whId, bn)
            .orElseThrow(() -> new IllegalStateException("插入余额后加锁失败"));
    }

    private ErpProductCategory lookupCategory(Long productId, ErpProductCategory fallback) {
        if (fallback != null) return fallback;
        return productRepo.findById(productId).map(ErpProduct::getCategory).orElse(null);
    }

    private ErpStockRecord buildRecord(StockBizType biz, ChangeRequest req, ChangeItem it,
                                       Long skuId, String batchNo, BigDecimal afterQty) {
        ErpStockRecord rec = new ErpStockRecord();
        rec.setBizType(biz.code);
        rec.setBizId(req.bizId());
        rec.setBizNo(req.bizNo());
        ErpProduct p = productRepo.findById(it.productId()).orElseThrow();
        rec.setProduct(p);
        rec.setSku(skuRepo.findById(skuId).orElseThrow());
        rec.setWarehouse(whRepo.findById(it.warehouseId()).orElseThrow());
        rec.setBatchNo(batchNo == null ? DEFAULT_BATCH : batchNo);
        rec.setQty(it.qty());
        rec.setAfterQty(afterQty);
        rec.setEventTime(LocalDateTime.now());
        rec.setRemark(req.remark());
        return rec;
    }
}
