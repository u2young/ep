package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsShipmentNotice;
import xyz.herz.ep.wms.enums.WmsDictEnums.ShipmentNoticeStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 出库通知关闭处理器。
 * <p>状态迁移:NEW(0) / PARTIAL_PICKED(10) → CLOSED(30)。
 * 已拣货(20)的通知不允许关闭(应自然结束)。
 */
@Component
public class WmsShipmentNoticeCloseHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CLOSE = "wms.shipment.close";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsShipmentNotice notice)) {
                    throw new IllegalStateException("仅支持出库通知单");
                }
                int cur = notice.getStatus() == null ? ShipmentNoticeStatus.NEW.code : notice.getStatus();
                if (cur != ShipmentNoticeStatus.NEW.code && cur != ShipmentNoticeStatus.PARTIAL_PICKED.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许关闭,仅 新建/部分拣货 可关闭");
                }
                notice.setStatus(ShipmentNoticeStatus.CLOSED.code);
                em.merge(notice);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CLOSE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private static String label(int code) {
        for (ShipmentNoticeStatus a : ShipmentNoticeStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
