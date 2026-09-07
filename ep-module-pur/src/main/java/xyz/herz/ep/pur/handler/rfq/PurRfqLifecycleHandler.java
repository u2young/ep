package xyz.herz.ep.pur.handler.rfq;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.pur.entity.rfq.PurRequestForQuotation;
import xyz.herz.ep.pur.enums.PurDictEnums.RfqStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 询价单生命周期行按钮处理器(参考 ERPNext Request for Quotation 工作流)。
 * <p>发送(DRAFT→SENT,校验截止日期未过期 + 采购员已指定)
 * / 收报价(SENT→RECEIVED,标记收到供应商回复,回写 receivedAt)
 * / 取消(DRAFT/SENT→CANCELLED,已收报价终态不可取消)。
 *
 * <p>语义:本模块只维护询价单状态机;供应商报价实体(PurSupplierQuotation)
 * 由采购员在收到回复后手工录入,并标记 isAccepted 选定中标方。
 */
@Component
public class PurRfqLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SEND = "pur.rfq.send";
    public static final String CODE_RECEIVE = "pur.rfq.receive";
    public static final String CODE_CANCEL = "pur.rfq.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SEND;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof PurRequestForQuotation doc)) {
                fail++; sb.append("仅支持询价单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SEND -> applySend(doc);
                    case CODE_RECEIVE -> applyReceive(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("询价单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 发送:草稿 → 已发送,校验截止日期有效 + 采购员已指定。 */
    private void applySend(PurRequestForQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != RfqStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可发送,当前状态码: " + st);
        }
        if (doc.getDueDate() == null) {
            throw new IllegalStateException("截止日期不能为空,无法发送询价");
        }
        if (doc.getDueDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("截止日期已过期,无法发送询价: " + doc.getDueDate());
        }
        if (doc.getBuyer() == null || doc.getBuyer().isBlank()) {
            throw new IllegalStateException("采购员不能为空,无法发送询价");
        }
        doc.setStatus(RfqStatus.SENT.code);
    }

    /** 收报价:已发送 → 已收报价,回写 receivedAt。 */
    private void applyReceive(PurRequestForQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != RfqStatus.SENT.code) {
            throw new IllegalStateException("仅已发送状态可收报价,当前状态码: " + st);
        }
        doc.setReceivedAt(LocalDateTime.now());
        doc.setStatus(RfqStatus.RECEIVED.code);
    }

    /** 取消:草稿/已发送 → 已取消;已收报价终态不可取消(已进入比价环节)。 */
    private void applyCancel(PurRequestForQuotation doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == RfqStatus.RECEIVED.code) {
            throw new IllegalStateException("已收报价不可取消(已进入比价环节),当前状态码: " + st);
        }
        if (st == RfqStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(RfqStatus.CANCELLED.code);
    }
}
