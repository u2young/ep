package xyz.herz.ep.qal.handler.inspection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.qal.entity.criteria.QalCriteria;
import xyz.herz.ep.qal.entity.inspection.QalInspection;
import xyz.herz.ep.qal.entity.inspection.QalInspectionItem;
import xyz.herz.ep.qal.entity.nc.QalNonConformance;
import xyz.herz.ep.qal.enums.QalDictEnums.InspectionStatus;
import xyz.herz.ep.qal.enums.QalDictEnums.NcStatus;
import xyz.herz.ep.qal.jpa.criteria.QalCriteriaRepository;
import xyz.herz.ep.qal.jpa.nc.QalNonConformanceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 质检单生命周期行按钮处理器(参考 ERPNext Quality Inspection 工作流)。
 * <p>提交(DRAFT→PENDING,按启用参数快照生成读数明细)
 * / 判定合格(PENDING→PASSED,要求全部读数合格)
 * / 判定不合格(PENDING→REJECTED,要求存在不合格读数,自动生成 N/C 单)
 * / 取消(DRAFT/PENDING→CANCELLED)。
 *
 * <p>读数判定规则:
 * <ul>
 *   <li>参数有上下限:reading 在 [lower, upper] 内自动合格,越界自动不合格</li>
 *   <li>参数无上下限(目测类):以检查员勾选的 isPass 为准</li>
 * </ul>
 */
@Component
public class QalInspectionLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_SUBMIT = "qal.insp.submit";
    public static final String CODE_PASS = "qal.insp.pass";
    public static final String CODE_FAIL = "qal.insp.fail";
    public static final String CODE_CANCEL = "qal.insp.cancel";

    @PersistenceContext
    private EntityManager em;

    @org.springframework.beans.factory.annotation.Autowired
    private QalCriteriaRepository criteriaRepo;

    @org.springframework.beans.factory.annotation.Autowired
    private QalNonConformanceRepository ncRepo;

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_SUBMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof QalInspection doc)) {
                fail++; sb.append("仅支持质检单实体; "); continue;
            }
            try {
                switch (code) {
                    case CODE_SUBMIT -> applySubmit(doc);
                    case CODE_PASS -> applyPass(doc);
                    case CODE_FAIL -> applyFail(doc);
                    case CODE_CANCEL -> applyCancel(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("质检单#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        return "成功 " + ok + " 失败 " + fail + (sb.length() > 0 ? " 详情: " + sb : "");
    }

    /** 提交:草稿 → 待检,按启用的质检参数快照生成读数明细。 */
    private void applySubmit(QalInspection doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != InspectionStatus.DRAFT.code) {
            throw new IllegalStateException("仅草稿可提交,当前状态码: " + st);
        }
        List<QalCriteria> enabled = criteriaRepo.findByStatus(
            xyz.herz.ep.qal.enums.QalDictEnums.EnableStatus.ENABLED.code);
        if (enabled.isEmpty()) {
            throw new IllegalStateException("无启用的质检参数标准,无法生成读数明细");
        }
        List<QalInspectionItem> snapshot = new ArrayList<>();
        for (QalCriteria c : enabled) {
            QalInspectionItem item = new QalInspectionItem();
            item.setInspection(doc);
            item.setCriteria(c);
            item.setCriteriaName(c.getName());
            item.setSpecText(buildSpecText(c));
            snapshot.add(item);
        }
        doc.getItems().clear();
        doc.getItems().addAll(snapshot);
        doc.setStatus(InspectionStatus.PENDING.code);
    }

    /** 判定合格:待检 → 合格,要求全部读数合格。 */
    private void applyPass(QalInspection doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != InspectionStatus.PENDING.code) {
            throw new IllegalStateException("仅待检状态可判定,当前状态码: " + st);
        }
        if (doc.getItems().isEmpty()) {
            throw new IllegalStateException("无读数明细,请先提交生成读数");
        }
        recalcAll(doc);
        for (QalInspectionItem item : doc.getItems()) {
            if (!Boolean.TRUE.equals(item.getIsPass())) {
                throw new IllegalStateException("读数[" + item.getCriteriaName()
                    + "]不合格,不能判定整体合格,请判不合格");
            }
        }
        markDecided(doc);
        doc.setStatus(InspectionStatus.PASSED.code);
    }

    /** 判定不合格:待检 → 不合格,要求存在不合格读数,并自动生成 N/C 单。 */
    private void applyFail(QalInspection doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != InspectionStatus.PENDING.code) {
            throw new IllegalStateException("仅待检状态可判定,当前状态码: " + st);
        }
        if (doc.getItems().isEmpty()) {
            throw new IllegalStateException("无读数明细,请先提交生成读数");
        }
        recalcAll(doc);
        List<String> failedNames = new ArrayList<>();
        for (QalInspectionItem item : doc.getItems()) {
            if (!Boolean.TRUE.equals(item.getIsPass())) failedNames.add(item.getCriteriaName());
        }
        if (failedNames.isEmpty()) {
            throw new IllegalStateException("全部读数合格,无需判定不合格");
        }
        markDecided(doc);
        doc.setStatus(InspectionStatus.REJECTED.code);
        // 自动生成不合格品处理单(幂等:已回写过则跳过)
        if (doc.getNcNo() == null || doc.getNcNo().isBlank()) {
            String ncNo = "NC-" + doc.getInspectionNo();
            QalNonConformance nc = new QalNonConformance();
            nc.setNcNo(ncNo);
            nc.setSourceInspectionNo(doc.getInspectionNo());
            nc.setItemCode(doc.getItemCode());
            nc.setItemName(doc.getItemName());
            nc.setBatchNo(doc.getBatchNo());
            nc.setQty(doc.getQty());
            nc.setDescription("质检单 " + doc.getInspectionNo() + " 不合格参数: "
                + String.join("、", failedNames));
            nc.setStatus(NcStatus.DRAFT.code);
            ncRepo.save(nc);
            doc.setNcNo(ncNo);
        }
    }

    /** 取消:草稿/待检 → 已取消;已判定(合格/不合格/已取消)不可取消。 */
    private void applyCancel(QalInspection doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == InspectionStatus.PASSED.code || st == InspectionStatus.REJECTED.code
            || st == InspectionStatus.CANCELLED.code) {
            throw new IllegalStateException("已判定的质检单不可取消,当前状态码: " + st);
        }
        doc.setStatus(InspectionStatus.CANCELLED.code);
    }

    // =================== 内部工具 ===================

    private static String buildSpecText(QalCriteria c) {
        if (c.getLowerLimit() == null && c.getUpperLimit() == null) {
            return "目测";
        }
        String lo = c.getLowerLimit() == null ? "-∞" : strip(c.getLowerLimit());
        String hi = c.getUpperLimit() == null ? "+∞" : strip(c.getUpperLimit());
        String txt = "[" + lo + ", " + hi + "]";
        return c.getUnit() == null || c.getUnit().isBlank() ? txt : txt + " " + c.getUnit();
    }

    private static String strip(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    /** 按规格上下限重算全部明细的 isPass(有上下限自动判,无上下限采信人工勾选)。 */
    private static void recalcAll(QalInspection doc) {
        for (QalInspectionItem item : doc.getItems()) {
            QalCriteria c = item.getCriteria();
            if (c == null || (c.getLowerLimit() == null && c.getUpperLimit() == null)) {
                // 目测类:采信人工勾选,未勾选视为不合格
                item.setIsPass(Boolean.TRUE.equals(item.getIsPass()));
                continue;
            }
            BigDecimal r = item.getReadingValue();
            boolean inRange = r != null
                && (c.getLowerLimit() == null || r.compareTo(c.getLowerLimit()) >= 0)
                && (c.getUpperLimit() == null || r.compareTo(c.getUpperLimit()) <= 0);
            item.setIsPass(inRange);
        }
    }

    private static void markDecided(QalInspection doc) {
        doc.setDecidedAt(LocalDateTime.now());
        if (doc.getInspectedDate() == null) {
            doc.setInspectedDate(LocalDate.now());
        }
        if (doc.getInspector() == null || doc.getInspector().isBlank()) {
            doc.setInspector("system");
        }
    }
}
