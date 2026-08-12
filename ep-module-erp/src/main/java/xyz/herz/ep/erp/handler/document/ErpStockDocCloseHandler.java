package xyz.herz.ep.erp.handler.document;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.erp.entity.document.ErpStockIn;
import xyz.herz.ep.erp.entity.document.ErpStockOut;
import xyz.herz.ep.erp.enums.ErpDictEnums.StockDocStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * 其他出入库单「关闭」共用行按钮处理器。
 * <p>状态迁移:0(草稿) / 1(已审核) → 2(已关闭)。已关闭的不能再关闭。
 */
@Component
public class ErpStockDocCloseHandler implements OperationHandler<Object, Object> {

    public static final String CODE_CLOSE = "erp.stockdoc.close";

    @PersistenceContext private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            try {
                if (row instanceof ErpStockIn doc) {
                    close(doc.getStatus(), doc::setStatus, doc.getNo(), ErpStockIn.class.getSimpleName());
                } else if (row instanceof ErpStockOut doc) {
                    close(doc.getStatus(), doc::setStatus, doc.getNo(), ErpStockOut.class.getSimpleName());
                } else {
                    throw new IllegalStateException("仅支持其他入库/出库单");
                }
                em.merge(row);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append(row.getClass().getSimpleName()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, CODE_CLOSE);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void close(Integer current, java.util.function.Consumer<Integer> setter,
                       String no, String type) {
        if (current == null) current = StockDocStatus.DRAFT.code;
        if (current == StockDocStatus.CLOSED.code) {
            throw new IllegalStateException(type + "#" + no + " 已是关闭状态");
        }
        setter.accept(StockDocStatus.CLOSED.code);
    }
}
