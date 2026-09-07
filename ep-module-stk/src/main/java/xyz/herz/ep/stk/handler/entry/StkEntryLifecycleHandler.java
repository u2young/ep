package xyz.herz.ep.stk.handler.entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.stk.entity.entry.StkStockEntry;
import xyz.herz.ep.stk.entity.entry.StkStockEntryItem;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryStatus;
import xyz.herz.ep.stk.enums.StkDictEnums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存出入库单生命周期行按钮处理器(参考 ERPNext Stock Entry 工作流)。
 * <p>提交(DRAFT→SUBMITTED,校验明细 + 仓库匹配类型 + 回写 totalQty/totalAmount/submittedAt)
 * / 取消(DRAFT/SUBMITTED→CANCELLED,已提交终态不可取消,因为已影响库存)。
 *
 * <p>仓库类型匹配规则:
 * <ul>
 *   <li>入库(MATERIAL_RECEIPT):只需目标仓</li>
 *   <li>出库(MATERIAL_ISSUE):只需来源仓</li>
 *   <li>移库/生产/翻包:来源仓 + 目标仓均需</li>
 * </ul>
 */
@Component
public class StkEntryLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "stk.entry.submit";
    public static final String CODE_CANCEL = "stk.entry.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof StkStockEntry doc)) {
                fail++; sb.append("仅支持库存出入库单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("出入库单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交,校验明细 + 仓库匹配 + 回写 totalQty/totalAmount/submittedAt。 */
    private void applySubmit(StkStockEntry doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != EntryStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        List<StkStockEntryItem> items = doc.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("出入库明细不能为空,无法提交");
        }
        // 仓库类型匹配校验
        int type = doc.getEntryType() != null ? doc.getEntryType() : -1;
        if (type == EntryType.MATERIAL_RECEIPT.code) {
            // 入库:只需目标仓
            if (isBlank(doc.getTargetWarehouseCode())) {
                throw new IllegalStateException("入库类型必须填写目标仓");
            }
        } else if (type == EntryType.MATERIAL_ISSUE.code) {
            // 出库:只需来源仓
            if (isBlank(doc.getSourceWarehouseCode())) {
                throw new IllegalStateException("出库类型必须填写来源仓");
            }
        } else {
            // 移库/生产/翻包:来源仓 + 目标仓均需
            if (isBlank(doc.getSourceWarehouseCode()) || isBlank(doc.getTargetWarehouseCode())) {
                throw new IllegalStateException("移库/生产/翻包类型必须同时填写来源仓和目标仓");
            }
        }
        // 回写明细小计 + 主单合计
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (StkStockEntryItem it : items) {
            if (it.getQty() == null) {
                throw new IllegalStateException("明细[" + it.getItemCode() + "]数量未填,无法提交");
            }
            if (it.getAmount() == null) {
                BigDecimal q = it.getQty();
                BigDecimal p = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                it.setAmount(q.multiply(p));
            }
            totalQty = totalQty.add(it.getQty());
            totalAmount = totalAmount.add(it.getAmount());
        }
        doc.setTotalQty(totalQty);
        doc.setTotalAmount(totalAmount);
        doc.setSubmittedAt(LocalDateTime.now());
        if (isBlank(doc.getOperator())) {
            doc.setOperator("system");
        }
        doc.setStatus(EntryStatus.SUBMITTED.code);
    }

    /** 取消:草稿/已提交 → 已取消;已取消不可再取消。 */
    private void applyCancel(StkStockEntry doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == EntryStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(EntryStatus.CANCELLED.code);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
