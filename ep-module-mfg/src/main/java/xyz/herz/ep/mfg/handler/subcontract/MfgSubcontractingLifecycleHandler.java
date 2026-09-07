package xyz.herz.ep.mfg.handler.subcontract;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.mfg.entity.subcontract.MfgSubcontractingOrder;
import xyz.herz.ep.mfg.enums.MfgDictEnums.SubcontractingStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

/**
 * 委外单生命周期行按钮处理器(下单/收货/结算/取消,四合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>SUBMIT:DRAFT(0) → ORDERED(1),校验 supplier/qty/rate,回写 totalAmount = qty * rate</li>
 *   <li>RECEIVE:ORDERED(1)/PARTIAL(2) → PARTIAL_RECEIVED(2)/FULL_RECEIVED(3),按 receivedQty 判定</li>
 *   <li>SETTLE:FULL_RECEIVED(3) → SETTLED(4)</li>
 *   <li>CANCEL:DRAFT(0)/ORDERED(1) → CANCELLED(5);已收货不可取消</li>
 * </ul>
 * 委外发料/收货的库存联动通过单独的 MfgStockEntry(ttype=SUBCONTRACT_ISSUE/RECEIPT)完成。
 */
@Component
public class MfgSubcontractingLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "mfg.subco.submit";
    public static final String CODE_RECEIVE = "mfg.subco.receive";
    public static final String CODE_SETTLE = "mfg.subco.settle";
    public static final String CODE_CANCEL = "mfg.subco.cancel";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof MfgSubcontractingOrder doc)) {
                fail++; sb.append("仅支持委外单; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_RECEIVE -> applyReceive(doc);
                    case CODE_SETTLE -> applySettle(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("委外#").append(doc.getNo()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applySubmit(MfgSubcontractingOrder doc) {
        Integer st = doc.getStatus();
        if (st == null || st != SubcontractingStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可以下单,当前状态=" + st);
        }
        if (doc.getSupplier() == null || doc.getSupplier().getId() == null) {
            throw new IllegalStateException("供应商不能为空");
        }
        BigDecimal qty = doc.getQty() == null ? BigDecimal.ZERO : doc.getQty();
        BigDecimal rate = doc.getRate() == null ? BigDecimal.ZERO : doc.getRate();
        if (qty.signum() <= 0) throw new IllegalStateException("委外数量必须 > 0");
        if (rate.signum() < 0) throw new IllegalStateException("加工费率不可为负");
        doc.setTotalAmount(qty.multiply(rate));
        doc.setStatus(SubcontractingStatus.ORDERED.code);
    }

    private void applyReceive(MfgSubcontractingOrder doc) {
        Integer st = doc.getStatus();
        if (st == null || (st != SubcontractingStatus.ORDERED.code
                && st != SubcontractingStatus.PARTIAL_RECEIVED.code)) {
            throw new IllegalStateException("只有已下单/部分到货状态可收货,当前状态=" + st);
        }
        BigDecimal received = doc.getReceivedQty() == null ? BigDecimal.ZERO : doc.getReceivedQty();
        BigDecimal planned = doc.getQty() == null ? BigDecimal.ZERO : doc.getQty();
        // 收货数量必须 > 0 且不超过计划数;按 received vs planned 判定 FULL/PARTIAL_RECEIVED
        // (调用方通过前置 setReceivedQty 声明本次累计已收货数量,Handler 据此推进状态)
        if (received.signum() <= 0) {
            throw new IllegalStateException("收货数量必须 > 0,当前 receivedQty=" + received);
        }
        if (received.compareTo(planned) > 0) {
            throw new IllegalStateException("收货数量超过计划数 " + planned + ",当前 receivedQty=" + received);
        }
        doc.setStatus(received.compareTo(planned) >= 0
            ? SubcontractingStatus.FULL_RECEIVED.code
            : SubcontractingStatus.PARTIAL_RECEIVED.code);
    }

    private void applySettle(MfgSubcontractingOrder doc) {
        Integer st = doc.getStatus();
        if (st == null || st != SubcontractingStatus.FULL_RECEIVED.code) {
            throw new IllegalStateException("只有全部到货状态可结算,当前状态=" + st);
        }
        doc.setStatus(SubcontractingStatus.SETTLED.code);
    }

    private void applyCancel(MfgSubcontractingOrder doc) {
        Integer st = doc.getStatus();
        if (st == null || st >= SubcontractingStatus.PARTIAL_RECEIVED.code) {
            throw new IllegalStateException("已收货的委外单不可取消,当前状态=" + st);
        }
        if (st == SubcontractingStatus.CANCELLED.code) {
            throw new IllegalStateException("委外单已取消");
        }
        doc.setStatus(SubcontractingStatus.CANCELLED.code);
    }
}
