package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsLocation;
import xyz.herz.ep.wms.entity.WmsPutaway;
import xyz.herz.ep.wms.entity.WmsPutawayItem;
import xyz.herz.ep.wms.entity.WmsStock;
import xyz.herz.ep.wms.entity.WmsStockMove;
import xyz.herz.ep.wms.enums.WmsDictEnums.PutawayStatus;
import xyz.herz.ep.wms.jpa.WmsStockMoveRepository;
import xyz.herz.ep.wms.jpa.WmsStockRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 上架单完成处理器。
 * <p>状态迁移:Putaway NEW(0) / PUTTING(10) → COMPLETED(20)。
 * 完成时:对每个明细在目标库位创建/更新 WmsStock,availableQty += qty,并写一条 PUTAWAY 流水。
 */
@Component
public class WmsPutawayCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "wms.putaway.complete";
    private static final String DEFAULT_BATCH = "-";

    @PersistenceContext
    private EntityManager em;
    private final WmsStockRepository stockRepo;
    private final WmsStockMoveRepository moveRepo;

    public WmsPutawayCompleteHandler(WmsStockRepository stockRepo, WmsStockMoveRepository moveRepo) {
        this.stockRepo = stockRepo;
        this.moveRepo = moveRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsPutaway putaway)) {
                    throw new IllegalStateException("仅支持上架单");
                }
                int cur = putaway.getStatus() == null ? PutawayStatus.NEW.code : putaway.getStatus();
                if (cur != PutawayStatus.NEW.code && cur != PutawayStatus.PUTTING.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许完成,仅 新建/上架中 可完成");
                }

                for (WmsPutawayItem item : putaway.getItems()) {
                    int qty = item.getQty() == null ? 0 : item.getQty();
                    if (qty <= 0) continue;
                    WmsStock stock = lockOrCreateStock(item.getLocationId(), item.getSkuCode());
                    stock.setAvailableQty((stock.getAvailableQty() == null ? 0 : stock.getAvailableQty()) + qty);
                    stockRepo.save(stock);

                    WmsStockMove move = new WmsStockMove();
                    move.setFromLocationId(null);
                    move.setToLocationId(item.getLocationId());
                    move.setSkuCode(item.getSkuCode());
                    move.setQty(qty);
                    move.setBizType("PUTAWAY");
                    move.setRemark("上架单 " + putaway.getNo());
                    moveRepo.save(move);
                }

                putaway.setStatus(PutawayStatus.COMPLETED.code);
                em.merge(putaway);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_COMPLETE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private WmsStock lockOrCreateStock(Long locationId, String skuCode) {
        return stockRepo.findByLocationSku(locationId, skuCode)
            .orElseGet(() -> {
                WmsLocation loc = em.find(WmsLocation.class, locationId);
                if (loc == null) {
                    throw new IllegalStateException("库位不存在: id=" + locationId);
                }
                WmsStock s = new WmsStock();
                s.setLocation(loc);
                s.setSkuCode(skuCode);
                s.setBatchNo(DEFAULT_BATCH);
                s.setAvailableQty(0);
                s.setLockedQty(0);
                s.setInTransitQty(0);
                s.setFrozenQty(0);
                return stockRepo.save(s);
            });
    }

    private static String label(int code) {
        for (PutawayStatus a : PutawayStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
