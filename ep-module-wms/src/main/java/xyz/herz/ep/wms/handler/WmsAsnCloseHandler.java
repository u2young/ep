package xyz.herz.ep.wms.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.wms.entity.WmsAsn;
import xyz.herz.ep.wms.enums.WmsDictEnums.AsnStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * ASN 关闭处理器。
 * <p>状态迁移:NEW(0) / PARTIAL_RECEIVED(10) → CLOSED(30)。
 * 已收货(20)的 ASN 不允许关闭(应自然结束)。
 */
@Component
public class WmsAsnCloseHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CLOSE = "wms.asn.close";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (!(row instanceof WmsAsn asn)) {
                    throw new IllegalStateException("仅支持 ASN 单据");
                }
                int cur = asn.getStatus() == null ? AsnStatus.NEW.code : asn.getStatus();
                if (cur != AsnStatus.NEW.code && cur != AsnStatus.PARTIAL_RECEIVED.code) {
                    throw new IllegalStateException("当前状态 " + label(cur) + " 不允许关闭,仅 新建/部分收货 可关闭");
                }
                asn.setStatus(AsnStatus.CLOSED.code);
                em.merge(asn);
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
        for (AsnStatus a : AsnStatus.values()) if (a.code == code) return a.label;
        return "UNKNOWN(" + code + ")";
    }
}
