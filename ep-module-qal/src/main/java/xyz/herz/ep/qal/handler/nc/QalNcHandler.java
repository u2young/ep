package xyz.herz.ep.qal.handler.nc;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.qal.entity.nc.QalNonConformance;
import xyz.herz.ep.qal.enums.QalDictEnums.NcStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 不合格品处理单生命周期行按钮处理器(参考 ERPNext Quality Non Conformance 工作流)。
 * <p>提交(草稿→已提交)/ 开始处理(已提交→处理中)
 * / 关闭(处理中→已关闭,要求处置方式 + 处理说明,回写关闭日期)
 * / 取消(未关闭→已取消)。
 */
@Component
public class QalNcHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "qal.nc.submit";
    public static final String CODE_PROCESS = "qal.nc.process";
    public static final String CODE_CLOSE = "qal.nc.close";
    public static final String CODE_CANCEL = "qal.nc.cancel";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof QalNonConformance doc)) {
                fail++; sb.append("仅支持不合格品处理实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_PROCESS -> applyProcess(doc);
                    case CODE_CLOSE -> applyClose(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("N/C#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 已提交。 */
    private void applySubmit(QalNonConformance doc) {
        int st = st(doc);
        if (st != NcStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        doc.setStatus(NcStatus.SUBMITTED.code);
    }

    /** 开始处理:已提交 → 处理中。 */
    private void applyProcess(QalNonConformance doc) {
        int st = st(doc);
        if (st != NcStatus.SUBMITTED.code) {
            throw new IllegalStateException("仅已提交状态可开始处理,当前状态码: " + st);
        }
        doc.setStatus(NcStatus.PROCESSING.code);
    }

    /** 关闭:处理中 → 已关闭,要求处置方式 + 处理说明,回写关闭日期。 */
    private void applyClose(QalNonConformance doc) {
        int st = st(doc);
        if (st != NcStatus.PROCESSING.code) {
            throw new IllegalStateException("仅处理中状态可关闭,当前状态码: " + st);
        }
        if (doc.getDisposition() == null) {
            throw new IllegalStateException("关闭前必须选择处置方式(退货/返工/让步接收/报废)");
        }
        if (doc.getProcessNote() == null || doc.getProcessNote().isBlank()) {
            throw new IllegalStateException("关闭前必须填写处理说明");
        }
        doc.setClosedDate(LocalDate.now());
        doc.setStatus(NcStatus.CLOSED.code);
    }

    /** 取消:未关闭 → 已取消;已关闭不可取消。 */
    private void applyCancel(QalNonConformance doc) {
        int st = st(doc);
        if (st == NcStatus.CLOSED.code) {
            throw new IllegalStateException("已关闭的 N/C 单不可取消");
        }
        if (st == NcStatus.CANCELLED.code) {
            throw new IllegalStateException("已取消不可再取消");
        }
        doc.setStatus(NcStatus.CANCELLED.code);
    }

    private static int st(QalNonConformance doc) {
        return doc.getStatus() != null ? doc.getStatus() : -1;
    }
}
