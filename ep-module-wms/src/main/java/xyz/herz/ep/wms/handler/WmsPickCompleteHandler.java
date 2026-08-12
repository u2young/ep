package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsPick;
import xyz.herz.ep.wms.entity.WmsPickItem;
import xyz.herz.ep.wms.entity.WmsShipmentItem;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;
import xyz.herz.ep.wms.entity.WmsStock;
import xyz.herz.ep.wms.entity.WmsStockMove;
import xyz.herz.ep.wms.enums.WmsDictEnums.PickStatus;
import xyz.herz.ep.wms.enums.WmsDictEnums.ShipmentNoticeStatus;
import xyz.herz.ep.wms.jpa.WmsShipmentItemRepository;
import xyz.herz.ep.wms.jpa.WmsStockMoveRepository;
import xyz.herz.ep.wms.jpa.WmsStockRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 拣货单完成处理器。
 * <p>状态迁移:Pick NEW(0) / PICKING(10) → COMPLETED(20)。
 * 完成时:
 * <ul>
 *   <li>对每个明细:WmsStock availableQty -= pickQty, lockedQty = max(0, lockedQty - pickQty)</li>
 *   <li>写一条 PICK 流水(fromLocationId=源库位, toLocationId=null)</li>
 *   <li>回写出库通知明细 pickedQty,并推进通知状态(部分拣货/已拣货)</li>
 * </ul>
 */
@Component
public class WmsPickCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "wms.pick.complete";

    @PersistenceContext
    private EntityManager em;
    private final WmsStockRepository stockRepo;
    private final WmsStockMoveRepository moveRepo;
    private final WmsShipmentItemRepository shipmentItemRepo;

    public WmsPickCompleteHandler(WmsStockRepository stockRepo,
                                 WmsStockMoveRepository moveRepo,
                                 WmsShipmentItemRepository shipmentItemRepo) {
        this.stockRepo = stockRepo;
        this.moveRepo = moveRepo;
        this.shipmentItemRepo = shipmentItemRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsPick pick)) {
                    throw new IllegalStateException("仅支持拣货单");
                }
                int cur = pick.getStatus() == null ? PickStatus.NEW.code : pick.getStatus();
                if (cur != PickStatus.NEW.code && cur != PickStatus.PICKING.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许完成,仅 新建/拣货中 可完成");
                }

                // 1. 扣减库存 + 写流水
                for (WmsPickItem item : pick.getItems()) {
                    int qty = item.getPickQty() == null ? 0 : item.getPickQty();
                    if (qty <= 0) continue;

                    WmsStock stock = stockRepo.findByLocationSku(item.getLocationId(), item.getSkuCode())
                        .orElseThrow(() -> new IllegalStateException(
                            "库位库存不存在: locationId=" + item.getLocationId() + ", sku=" + item.getSkuCode()));

                    int available = stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
                    if (available < qty) {
                        throw new IllegalStateException(
                            "库存不足: sku=" + item.getSkuCode() + ", 需要=" + qty + ", 可用=" + available);
                    }
                    stock.setAvailableQty(available - qty);

                    int locked = stock.getLockedQty() == null ? 0 : stock.getLockedQty();
                    stock.setLockedQty(Math.max(0, locked - qty));

                    stockRepo.save(stock);

                    WmsStockMove move = new WmsStockMove();
                    move.setFromLocationId(item.getLocationId());
                    move.setToLocationId(null);
                    move.setSkuCode(item.getSkuCode());
                    move.setQty(qty);
                    move.setBizType("PICK");
                    move.setRemark("拣货单 " + pick.getNo());
                    moveRepo.save(move);
                }

                // 2. 回写出库通知明细 pickedQty + 推进通知状态
                if (pick.getNoticeId() != null) {
                    backWriteShipmentNotice(pick);
                }

                pick.setStatus(PickStatus.COMPLETED.code);
                em.merge(pick);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_COMPLETE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void backWriteShipmentNotice(WmsPick pick) {
        Long noticeId = pick.getNoticeId();
        WmsShipmentNotice notice = em.find(WmsShipmentNotice.class, noticeId);
        if (notice == null) {
            throw new IllegalStateException("关联的出库通知不存在: id=" + noticeId);
        }
        for (WmsPickItem item : pick.getItems()) {
            int qty = item.getPickQty() == null ? 0 : item.getPickQty();
            if (qty <= 0) continue;
            WmsShipmentItem si = shipmentItemRepo.findByNoticeAndSku(noticeId, item.getSkuCode())
                .orElse(null);
            if (si == null) {
                throw new IllegalStateException("出库通知明细中找不到 SKU: " + item.getSkuCode());
            }
            si.setPickedQty((si.getPickedQty() == null ? 0 : si.getPickedQty()) + qty);
            em.merge(si);
        }
        // 根据拣货进度推进通知状态
        List<WmsShipmentItem> noticeItems = shipmentItemRepo.findByNoticeId(noticeId);
        boolean allFull = true;
        boolean anyPicked = false;
        for (WmsShipmentItem si : noticeItems) {
            int exp = si.getExpectedQty() == null ? 0 : si.getExpectedQty();
            int picked = si.getPickedQty() == null ? 0 : si.getPickedQty();
            if (picked < exp) allFull = false;
            if (picked > 0) anyPicked = true;
        }
        int target = notice.getStatus();
        if (allFull) {
            target = ShipmentNoticeStatus.PICKED.code;
        } else if (anyPicked) {
            target = ShipmentNoticeStatus.PARTIAL_PICKED.code;
        }
        // 不允许从 CLOSED 退回
        if (notice.getStatus() != null && notice.getStatus() >= ShipmentNoticeStatus.CLOSED.code) {
            target = notice.getStatus();
        }
        notice.setStatus(target);
        em.merge(notice);
    }

    private static String label(int code) {
        for (PickStatus a : PickStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
