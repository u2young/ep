package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsAsn;
import xyz.herz.ep.wms.entity.WmsAsnItem;
import xyz.herz.ep.wms.entity.WmsReceipt;
import xyz.herz.ep.wms.entity.WmsReceiptItem;
import xyz.herz.ep.wms.enums.WmsDictEnums.AsnStatus;
import xyz.herz.ep.wms.enums.WmsDictEnums.ReceiptStatus;
import xyz.herz.ep.wms.jpa.WmsAsnItemRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 收货单完成处理器。
 * <p>状态迁移:Receipt NEW(0) / RECEIVING(10) → COMPLETED(20)。
 * 完成时回写 ASN 明细 receivedQty,并根据 ASN 收货进度推进 ASN 状态:
 * <ul>
 *   <li>所有明细 receivedQty ≥ expectedQty → ASN = RECEIVED(20)</li>
 *   <li>任一明细 receivedQty > 0 → ASN = PARTIAL_RECEIVED(10)</li>
 * </ul>
 */
@Component
public class WmsReceiptCompleteHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMPLETE = "wms.receipt.complete";

    @PersistenceContext
    private EntityManager em;
    private final WmsAsnItemRepository asnItemRepo;

    public WmsReceiptCompleteHandler(WmsAsnItemRepository asnItemRepo) {
        this.asnItemRepo = asnItemRepo;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsReceipt receipt)) {
                    throw new IllegalStateException("仅支持收货单");
                }
                int cur = receipt.getStatus() == null ? ReceiptStatus.NEW.code : receipt.getStatus();
                if (cur != ReceiptStatus.NEW.code && cur != ReceiptStatus.RECEIVING.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许完成,仅 新建/收货中 可完成");
                }

                // 回写 ASN 明细
                if (receipt.getAsnId() != null) {
                    backWriteAsn(receipt);
                }

                receipt.setStatus(ReceiptStatus.COMPLETED.code);
                em.merge(receipt);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_COMPLETE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void backWriteAsn(WmsReceipt receipt) {
        Long asnId = receipt.getAsnId();
        WmsAsn asn = em.find(WmsAsn.class, asnId);
        if (asn == null) {
            throw new IllegalStateException("关联的 ASN 不存在: id=" + asnId);
        }
        // 回写每个收货明细到 ASN 明细
        for (WmsReceiptItem ri : receipt.getItems()) {
            WmsAsnItem ai = asnItemRepo.findByAsnAndSku(asnId, ri.getSkuCode()).orElse(null);
            if (ai == null) {
                throw new IllegalStateException("ASN 明细中找不到 SKU: " + ri.getSkuCode());
            }
            int recv = ri.getReceivedQty() == null ? 0 : ri.getReceivedQty();
            ai.setReceivedQty((ai.getReceivedQty() == null ? 0 : ai.getReceivedQty()) + recv);
            em.merge(ai);
        }
        // 根据 ASN 明细收货进度推进 ASN 状态
        List<WmsAsnItem> asnItems = asnItemRepo.findByAsnId(asnId);
        boolean allFull = true;
        boolean anyReceived = false;
        for (WmsAsnItem ai : asnItems) {
            int exp = ai.getExpectedQty() == null ? 0 : ai.getExpectedQty();
            int rcv = ai.getReceivedQty() == null ? 0 : ai.getReceivedQty();
            if (rcv < exp) allFull = false;
            if (rcv > 0) anyReceived = true;
        }
        int targetAsnStatus = asn.getStatus();
        if (allFull) {
            targetAsnStatus = AsnStatus.RECEIVED.code;
        } else if (anyReceived) {
            targetAsnStatus = AsnStatus.PARTIAL_RECEIVED.code;
        }
        // 不允许从 RECEIVED/CLOSED 退回
        if (asn.getStatus() != null && asn.getStatus() >= AsnStatus.RECEIVED.code) {
            targetAsnStatus = asn.getStatus();
        }
        asn.setStatus(targetAsnStatus);
        em.merge(asn);
    }

    private static String label(int code) {
        for (ReceiptStatus a : ReceiptStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
