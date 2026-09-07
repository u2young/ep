package xyz.herz.ep.qal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import xyz.herz.ep.qal.entity.criteria.QalCriteria;
import xyz.herz.ep.qal.entity.inspection.QalInspection;
import xyz.herz.ep.qal.entity.inspection.QalInspectionItem;
import xyz.herz.ep.qal.entity.nc.QalNonConformance;
import xyz.herz.ep.qal.entity.feedback.QalFeedback;
import xyz.herz.ep.qal.enums.QalDictEnums.EnableStatus;
import xyz.herz.ep.qal.enums.QalDictEnums.InspectionStatus;
import xyz.herz.ep.qal.enums.QalDictEnums.NcDisposition;
import xyz.herz.ep.qal.enums.QalDictEnums.NcStatus;
import xyz.herz.ep.qal.handler.inspection.QalInspectionLifecycleHandler;
import xyz.herz.ep.qal.handler.nc.QalNcHandler;
import xyz.herz.ep.qal.jpa.criteria.QalCriteriaRepository;
import xyz.herz.ep.qal.jpa.feedback.QalFeedbackRepository;
import xyz.herz.ep.qal.jpa.inspection.QalInspectionRepository;
import xyz.herz.ep.qal.jpa.nc.QalNonConformanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 质量管理模块冒烟测试(参考 ERPNext Quality Management DocType 移植后的原生化验证)。
 * <p>6 个用例覆盖 T-H 验收矩阵:
 * <ol>
 *   <li>Qal1 参数标准主数据:数值型(尺寸/重量)+ 目测型(外观) + 启停查询</li>
 *   <li>Qal2 质检单提交自动快照:从启用参数生成读数明细 + specText 规格</li>
 *   <li>Qal3 判定合格/不合格:全部读数在规格内判合格;越界判不合格并自动生成 N/C 单</li>
 *   <li>Qal4 N/C 生命周期:提交→处理→关闭(处置方式+处理说明必填,回写关闭日期)</li>
 *   <li>Qal5 状态机守卫:草稿不可判定/已判定不可再判/已关闭不可取消/status 禁止表单直改</li>
 *   <li>Qal6 客户反馈 CRUD:评分/处理回写/查询</li>
 * </ol>
 */
@SpringBootTest(classes = QalTestApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class QalSmokeTests {

    @Autowired QalCriteriaRepository criteriaRepo;
    @Autowired QalInspectionRepository inspRepo;
    @Autowired QalNonConformanceRepository ncRepo;
    @Autowired QalFeedbackRepository feedbackRepo;
    @Autowired QalInspectionLifecycleHandler inspLifecycle;
    @Autowired QalNcHandler ncHandler;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    /** 3 条参数标准:尺寸 [9.5,10.5] mm / 重量 [95,105] g / 外观 目测。 */
    private void seedCriteria() {
        QalCriteria dim = new QalCriteria();
        dim.setCode("DIM"); dim.setName("外径尺寸");
        dim.setUnit("mm"); dim.setLowerLimit(bd("9.5")); dim.setUpperLimit(bd("10.5"));
        dim.setIsMust(true); dim.setStatus(EnableStatus.ENABLED.code);
        criteriaRepo.save(dim);

        QalCriteria wt = new QalCriteria();
        wt.setCode("WT"); wt.setName("单件重量");
        wt.setUnit("g"); wt.setLowerLimit(bd("95")); wt.setUpperLimit(bd("105"));
        wt.setIsMust(true); wt.setStatus(EnableStatus.ENABLED.code);
        criteriaRepo.save(wt);

        QalCriteria ap = new QalCriteria();
        ap.setCode("APPEAR"); ap.setName("外观");
        ap.setIsMust(true); ap.setStatus(EnableStatus.ENABLED.code);
        criteriaRepo.save(ap);
    }

    private QalInspection buildInspection(String no) {
        QalInspection insp = new QalInspection();
        insp.setInspectionNo(no);
        insp.setSourceType("PurchaseReceipt");
        insp.setSourceNo("PR-T-001");
        insp.setItemCode("ITEM-001");
        insp.setItemName("测试轴套");
        insp.setBatchNo("B2026-001");
        insp.setQty(bd("100"));
        insp.setSampleQty(bd("5"));
        insp.setStatus(InspectionStatus.DRAFT.code);
        return inspRepo.save(insp);
    }

    private QalInspection submitInspection(QalInspection insp) {
        String r = inspLifecycle.exec(List.of(insp), null,
            new String[]{QalInspectionLifecycleHandler.CODE_SUBMIT});
        assertTrue(r.contains("成功 1"), "提交应成功,实际=" + r);
        return inspRepo.findById(insp.getId()).orElseThrow();
    }

    // =================== (1) 参数标准主数据 ===================
    @Test
    void qal1_criteria_master() {
        seedCriteria();

        List<QalCriteria> enabled = criteriaRepo.findByStatus(EnableStatus.ENABLED.code);
        assertEquals(3, enabled.size(), "启用参数应为 3 条");
        assertTrue(criteriaRepo.findByCode("DIM").isPresent(), "DIM 参数应存在");

        QalCriteria dim = criteriaRepo.findByCode("DIM").orElseThrow();
        assertEquals(0, dim.getLowerLimit().compareTo(bd("9.5")), "DIM 下限=9.5");
        assertEquals(0, dim.getUpperLimit().compareTo(bd("10.5")), "DIM 上限=10.5");
        assertEquals("mm", dim.getUnit(), "DIM 单位=mm");

        QalCriteria ap = criteriaRepo.findByCode("APPEAR").orElseThrow();
        assertNull(ap.getLowerLimit(), "外观参数无下限");
        assertNull(ap.getUpperLimit(), "外观参数无上限(目测类)");
    }

    // =================== (2) 质检单提交自动快照 ===================
    @Test
    void qal2_inspection_submit_snapshot() {
        seedCriteria();
        QalInspection insp = submitInspection(buildInspection("INSP-T-001"));

        assertEquals(InspectionStatus.PENDING.code, insp.getStatus(), "提交后状态=待检");
        List<QalInspectionItem> items = insp.getItems();
        assertEquals(3, items.size(), "读数明细应按启用参数快照 3 行");
        assertEquals("外径尺寸", items.get(0).getCriteriaName(), "明细应快照参数名称");
        assertEquals("[9.5, 10.5] mm", items.get(0).getSpecText(), "数值型规格快照");
        assertEquals("目测", items.get(2).getSpecText(), "目测型规格快照");
        assertNull(items.get(0).getReadingValue(), "读数初始为空");
    }

    // =================== (3) 判定合格/不合格 + 自动 N/C ===================
    @Test
    void qal3_inspection_pass_and_fail() {
        seedCriteria();

        // (a) 全部读数合格 → 判定合格
        QalInspection pass = submitInspection(buildInspection("INSP-T-002"));
        pass.getItems().get(0).setReadingValue(bd("10.0"));   // [9.5,10.5] 内
        pass.getItems().get(1).setReadingValue(bd("100"));    // [95,105] 内
        pass.getItems().get(2).setIsPass(true);               // 目测
        String passR = inspLifecycle.exec(List.of(pass), null,
            new String[]{QalInspectionLifecycleHandler.CODE_PASS});
        assertTrue(passR.contains("成功 1"), "判定合格应成功,实际=" + passR);
        QalInspection passed = inspRepo.findById(pass.getId()).orElseThrow();
        assertEquals(InspectionStatus.PASSED.code, passed.getStatus(), "状态=合格");
        assertNotNull(passed.getDecidedAt(), "判定时间应回写");
        assertNull(passed.getNcNo(), "合格不应生成 N/C 单");

        // (b) 读数越界 → 判定不合格 + 自动生成 N/C 单
        QalInspection fail = submitInspection(buildInspection("INSP-T-003"));
        fail.getItems().get(0).setReadingValue(bd("11.0"));   // 越上限
        fail.getItems().get(1).setReadingValue(bd("100"));
        fail.getItems().get(2).setIsPass(true);
        String failR = inspLifecycle.exec(List.of(fail), null,
            new String[]{QalInspectionLifecycleHandler.CODE_FAIL});
        assertTrue(failR.contains("成功 1"), "判定不合格应成功,实际=" + failR);
        QalInspection rejected = inspRepo.findById(fail.getId()).orElseThrow();
        assertEquals(InspectionStatus.REJECTED.code, rejected.getStatus(), "状态=不合格");
        assertEquals("NC-INSP-T-003", rejected.getNcNo(), "N/C 单号应回写");

        List<QalNonConformance> ncs = ncRepo.findBySourceInspectionNo("INSP-T-003");
        assertEquals(1, ncs.size(), "应自动生成 1 张 N/C 单");
        QalNonConformance nc = ncs.get(0);
        assertEquals(NcStatus.DRAFT.code, nc.getStatus(), "N/C 初始=草稿");
        assertEquals("ITEM-001", nc.getItemCode(), "N/C 快照物料编码");
        assertEquals(0, nc.getQty().compareTo(bd("100")), "N/C 快照不合格数量");
        assertTrue(nc.getDescription().contains("外径尺寸"),
            "N/C 描述应含不合格参数名,实际=" + nc.getDescription());

        // (c) 全部合格却判不合格应被拒
        QalInspection good = submitInspection(buildInspection("INSP-T-004"));
        good.getItems().get(0).setReadingValue(bd("10.0"));
        good.getItems().get(1).setReadingValue(bd("100"));
        good.getItems().get(2).setIsPass(true);
        String wrongFail = inspLifecycle.exec(List.of(good), null,
            new String[]{QalInspectionLifecycleHandler.CODE_FAIL});
        assertTrue(wrongFail.contains("失败 1"), "全部合格判不合格应被拒,实际=" + wrongFail);
    }

    // =================== (4) N/C 生命周期 ===================
    @Test
    void qal4_nc_lifecycle() {
        seedCriteria();
        QalInspection insp = submitInspection(buildInspection("INSP-T-005"));
        insp.getItems().get(0).setReadingValue(bd("11.0"));
        insp.getItems().get(1).setReadingValue(bd("100"));
        insp.getItems().get(2).setIsPass(true);
        inspLifecycle.exec(List.of(insp), null,
            new String[]{QalInspectionLifecycleHandler.CODE_FAIL});
        QalNonConformance nc = ncRepo.findByNcNo("NC-INSP-T-005").orElseThrow();

        // 草稿 → 已提交 → 处理中 → 关闭
        String subR = ncHandler.exec(List.of(nc), null, new String[]{QalNcHandler.CODE_SUBMIT});
        assertTrue(subR.contains("成功 1"), "N/C 提交应成功,实际=" + subR);
        String procR = ncHandler.exec(List.of(nc), null, new String[]{QalNcHandler.CODE_PROCESS});
        assertTrue(procR.contains("成功 1"), "N/C 开始处理应成功,实际=" + procR);

        // 关闭守卫:未选处置方式应被拒
        QalNonConformance processing = ncRepo.findById(nc.getId()).orElseThrow();
        String closeNoDisp = ncHandler.exec(List.of(processing), null,
            new String[]{QalNcHandler.CODE_CLOSE});
        assertTrue(closeNoDisp.contains("失败 1"), "未选处置方式关闭应被拒,实际=" + closeNoDisp);

        // 关闭守卫:无处理说明应被拒
        processing.setDisposition(NcDisposition.REWORK.code);
        String closeNoNote = ncHandler.exec(List.of(processing), null,
            new String[]{QalNcHandler.CODE_CLOSE});
        assertTrue(closeNoNote.contains("失败 1"), "无处理说明关闭应被拒,实际=" + closeNoNote);

        // 正常关闭
        processing.setProcessNote("返工后复检合格");
        String closeR = ncHandler.exec(List.of(processing), null,
            new String[]{QalNcHandler.CODE_CLOSE});
        assertTrue(closeR.contains("成功 1"), "N/C 关闭应成功,实际=" + closeR);
        QalNonConformance closed = ncRepo.findById(nc.getId()).orElseThrow();
        assertEquals(NcStatus.CLOSED.code, closed.getStatus(), "N/C 状态=已关闭");
        assertEquals(NcDisposition.REWORK.code, closed.getDisposition(), "处置方式=返工");
        assertNotNull(closed.getClosedDate(), "关闭日期应回写");
    }

    // =================== (5) 状态机守卫 ===================
    @Test
    void qal5_state_machine_guard() {
        seedCriteria();

        // (a) 草稿直接判定应被拒(须先提交)
        QalInspection draft = buildInspection("INSP-T-006");
        String passDraft = inspLifecycle.exec(List.of(draft), null,
            new String[]{QalInspectionLifecycleHandler.CODE_PASS});
        assertTrue(passDraft.contains("失败 1"), "草稿判定应被拒,实际=" + passDraft);

        // (b) 已判定(合格)不可再判定/取消
        QalInspection pass = submitInspection(buildInspection("INSP-T-007"));
        pass.getItems().get(0).setReadingValue(bd("10.0"));
        pass.getItems().get(1).setReadingValue(bd("100"));
        pass.getItems().get(2).setIsPass(true);
        inspLifecycle.exec(List.of(pass), null,
            new String[]{QalInspectionLifecycleHandler.CODE_PASS});
        QalInspection passed = inspRepo.findById(pass.getId()).orElseThrow();
        String rePass = inspLifecycle.exec(List.of(passed), null,
            new String[]{QalInspectionLifecycleHandler.CODE_PASS});
        assertTrue(rePass.contains("失败 1"), "已合格再判定应被拒,实际=" + rePass);
        String cancelPassed = inspLifecycle.exec(List.of(passed), null,
            new String[]{QalInspectionLifecycleHandler.CODE_CANCEL});
        assertTrue(cancelPassed.contains("失败 1"), "已合格取消应被拒,实际=" + cancelPassed);

        // (c) 已关闭 N/C 不可取消
        QalInspection insp = submitInspection(buildInspection("INSP-T-008"));
        insp.getItems().get(0).setReadingValue(bd("8.0"));   // 越下限
        insp.getItems().get(1).setReadingValue(bd("100"));
        insp.getItems().get(2).setIsPass(true);
        inspLifecycle.exec(List.of(insp), null,
            new String[]{QalInspectionLifecycleHandler.CODE_FAIL});
        QalNonConformance nc = ncRepo.findByNcNo("NC-INSP-T-008").orElseThrow();
        ncHandler.exec(List.of(nc), null, new String[]{QalNcHandler.CODE_SUBMIT});
        ncHandler.exec(List.of(nc), null, new String[]{QalNcHandler.CODE_PROCESS});
        nc.setDisposition(NcDisposition.SCRAP.code);
        nc.setProcessNote("报废处理");
        ncHandler.exec(List.of(nc), null, new String[]{QalNcHandler.CODE_CLOSE});
        QalNonConformance closed = ncRepo.findById(nc.getId()).orElseThrow();
        String cancelClosed = ncHandler.exec(List.of(closed), null,
            new String[]{QalNcHandler.CODE_CANCEL});
        assertTrue(cancelClosed.contains("失败 1"), "已关闭 N/C 取消应被拒,实际=" + cancelClosed);

        // (d) DataProxy beforeUpdate:status 字段禁止表单直改(非草稿抛异常)
        QalInspection.Proxy proxy = new QalInspection.Proxy();
        QalInspection draftE = new QalInspection();
        draftE.setStatus(InspectionStatus.DRAFT.code);
        assertDoesNotThrow(() -> proxy.beforeUpdate(draftE), "草稿状态允许表单编辑");

        QalInspection pendingE = new QalInspection();
        pendingE.setStatus(InspectionStatus.PENDING.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(pendingE),
            "待检状态修改应抛异常:status 禁止表单直改");

        QalInspection passedE = new QalInspection();
        passedE.setStatus(InspectionStatus.PASSED.code);
        assertThrows(IllegalArgumentException.class, () -> proxy.beforeUpdate(passedE),
            "合格状态修改应抛异常:status 禁止表单直改");
    }

    // =================== (6) 客户反馈 CRUD ===================
    @Test
    void qal6_feedback_crud() {
        QalFeedback f1 = new QalFeedback();
        f1.setTitle("轴套尺寸偏大");
        f1.setCustomerName("客户-A");
        f1.setItemCode("ITEM-001");
        f1.setRating(2);
        f1.setContent("收到的轴套外径超出图纸公差,请核查");
        f1.setIsHandled(false);
        feedbackRepo.save(f1);

        QalFeedback f2 = new QalFeedback();
        f2.setTitle("包装破损");
        f2.setCustomerName("客户-B");
        f2.setItemCode("ITEM-002");
        f2.setRating(4);
        f2.setContent("个别包装盒破损,产品本身完好");
        f2.setIsHandled(false);
        feedbackRepo.save(f2);

        assertEquals(2, feedbackRepo.findByIsHandled(false).size(), "未处理反馈应 2 条");

        // 处理回写
        f1.setIsHandled(true);
        f1.setHandledAt(LocalDateTime.now());
        f1.setReply("已补发合格品并排查产线模具");
        feedbackRepo.save(f1);

        assertEquals(1, feedbackRepo.findByIsHandled(false).size(), "未处理反馈应剩 1 条");
        assertEquals(1, feedbackRepo.findByIsHandled(true).size(), "已处理反馈应 1 条");
        assertEquals(1, feedbackRepo.findByCustomerName("客户-A").size(), "客户-A 反馈应 1 条");
        assertTrue(feedbackRepo.findByCustomerName("客户-A").get(0).getIsHandled(),
            "客户-A 反馈应已处理");
    }
}
