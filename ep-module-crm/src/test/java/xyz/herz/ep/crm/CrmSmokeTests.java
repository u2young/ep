package xyz.herz.ep.crm;

import xyz.herz.ep.crm.core.CrmFollowService;
import xyz.herz.ep.crm.entity.*;
import xyz.herz.ep.crm.enums.CrmDictEnums;
import xyz.herz.ep.crm.handler.CrmBusinessAdvanceHandler;
import xyz.herz.ep.crm.handler.CrmBusinessEndHandler;
import xyz.herz.ep.crm.handler.CrmClueTransformHandler;
import xyz.herz.ep.crm.handler.CrmContractEffectHandler;
import xyz.herz.ep.crm.handler.CrmContractVoidHandler;
import xyz.herz.ep.crm.handler.CrmCustomerClaimHandler;
import xyz.herz.ep.crm.handler.CrmCustomerTransferHandler;
import xyz.herz.ep.crm.handler.CrmReceivableConfirmHandler;
import xyz.herz.ep.crm.jpa.*;
import xyz.herz.ep.crm.job.CrmCustomerPoolRecycleJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRM MVP 冒烟单测(全链路)。
 * 流程:线索 → 转化为客户 → 认领 → 设置状态组/阶段 → 商机 → 推进两次 → 赢单(副作用:客户 dealStatus=1)
 * 另附带:跟进记录刷新主体字段、公海回收(配置后客户释放回公海)。
 *
 * H2 内存库,erupt schema 用 spring.jpa.hibernate.ddl-auto=update。
 */
@SpringBootTest(classes = CrmTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class CrmSmokeTests {

    @Autowired CrmClueRepository clueRepo;
    @Autowired CrmCustomerRepository customerRepo;
    @Autowired CrmContactRepository contactRepo;
    @Autowired CrmBusinessStatusTypeRepository statusTypeRepo;
    @Autowired CrmBusinessStatusRepository statusRepo;
    @Autowired CrmBusinessRepository businessRepo;
    @Autowired CrmFollowUpRecordRepository followRepo;
    @Autowired CrmCustomerPoolConfigRepository poolRepo;
    @Autowired CrmTeamMemberRepository teamRepo;
    @Autowired CrmContractRepository contractRepo;
    @Autowired CrmReceivablePlanRepository planRepo;
    @Autowired CrmReceivableRecordRepository recordRepo;

    @Autowired CrmClueTransformHandler clueTransformHandler;
    @Autowired CrmCustomerClaimHandler customerClaimHandler;
    @Autowired CrmCustomerTransferHandler customerTransferHandler;
    @Autowired CrmBusinessAdvanceHandler businessAdvanceHandler;
    @Autowired CrmBusinessEndHandler businessEndHandler;
    @Autowired CrmFollowService followService;
    @Autowired CrmCustomerPoolRecycleJob recycleJob;
    @Autowired CrmContractEffectHandler contractEffectHandler;
    @Autowired CrmContractVoidHandler contractVoidHandler;
    @Autowired CrmReceivableConfirmHandler receivableConfirmHandler;

    /** 1. 线索 -> 转化客户 */
    @Test
    void clue_to_customer_transform() {
        CrmClue c = new CrmClue();
        c.setName("张小明线索");
        c.setOwnerUserId(1L);
        c.setMobile("13800000001");
        c.setLevel(CrmDictEnums.CustomerLevel.A.code);
        clueRepo.save(c);
        assertNotNull(c.getId());
        assertEquals(0, c.getTransformStatus());

        String r = clueTransformHandler.exec(List.of(c), "", new String[]{});
        assertTrue(r.contains("成功 1"), () -> "转化失败:" + r);
        // reload
        CrmClue re = clueRepo.findById(c.getId()).orElseThrow();
        assertEquals(CrmDictEnums.ClueTransformStatus.TRANSFORMED.code, re.getTransformStatus());
        assertNotNull(re.getCustomerId(), "转化后必须回填 customerId");

        CrmCustomer cu = customerRepo.findById(re.getCustomerId()).orElseThrow();
        assertEquals("张小明线索", cu.getName());
        assertEquals("13800000001", cu.getMobile());
        assertEquals(1L, cu.getOwnerUserId());
        assertEquals(CrmDictEnums.DealStatus.NOT_DEALT.code, cu.getDealStatus());
    }

    /** 2. 公海客户认领 */
    @Test
    void customer_sea_claim() {
        CrmCustomer cu = new CrmCustomer();
        cu.setName("公海客户A");
        customerRepo.save(cu);
        assertNull(cu.getOwnerUserId());  // 为 null 即公海

        String r = customerClaimHandler.exec(List.of(cu), "", new String[]{});
        assertTrue(r.contains("成功 1"), () -> "认领失败:" + r);
        CrmCustomer re = customerRepo.findById(cu.getId()).orElseThrow();
        assertNotNull(re.getOwnerUserId(), "认领后必须有 ownerUserId");
        assertNotNull(re.getOwnerTime());
    }

    /** 3. 锁定/解锁/标记成交 */
    @Test
    void customer_lock_unlock_deal() {
        CrmCustomer cu = new CrmCustomer();
        cu.setName("客户B");
        cu.setOwnerUserId(1L);
        customerRepo.save(cu);

        String r1 = customerTransferHandler.exec(List.of(cu), "", new String[]{"MARK_LOCK"});
        assertTrue(r1.contains("成功 1"), r1);
        assertEquals(CrmDictEnums.LockStatus.LOCKED.code,
            customerRepo.findById(cu.getId()).orElseThrow().getLockStatus());

        String r2 = customerTransferHandler.exec(List.of(cu), "", new String[]{"MARK_UNLOCK"});
        assertTrue(r2.contains("成功 1"), r2);

        String r3 = customerTransferHandler.exec(List.of(cu), "", new String[]{"MARK_DEAL"});
        assertTrue(r3.contains("成功 1"), r3);
        assertEquals(CrmDictEnums.DealStatus.DEALT.code,
            customerRepo.findById(cu.getId()).orElseThrow().getDealStatus());
    }

    /** 4. 跟进记录 -> 主体最后跟进信息被刷新 */
    @Test
    void follow_up_updates_summary() {
        CrmClue clue = new CrmClue();
        clue.setName("跟进线索");
        clueRepo.save(clue);

        followService.createFollow(
            CrmDictEnums.FollowBizType.CLUE, clue.getId(),
            CrmDictEnums.FollowWay.PHONE.code, "电话沟通,明确有意向", null, null
        );

        CrmClue re = clueRepo.findById(clue.getId()).orElseThrow();
        assertEquals(CrmDictEnums.ClueFollowStatus.FOLLOWED.code, re.getFollowUpStatus());
        assertNotNull(re.getContactLastTime());
        assertTrue(re.getContactLastContent().startsWith("电话沟通"));
    }

    /** 5. 商机三层状态机:阶段推进 -> 赢单(客户自动成交) */
    @Test
    void business_advance_and_win_side_effect() {
        // 先造客户
        CrmCustomer cu = new CrmCustomer();
        cu.setName("商机客户C");
        cu.setOwnerUserId(1L);
        customerRepo.save(cu);

        // 造状态组 + 4 阶段(10% → 30% → 60% → 90%)
        CrmBusinessStatusType t = new CrmBusinessStatusType();
        t.setName("标准销售");
        statusTypeRepo.save(t);
        CrmBusinessStatus s1 = mkStatus(t.getId(), "需求分析", 10, 1);
        CrmBusinessStatus s2 = mkStatus(t.getId(), "方案报价", 30, 2);
        CrmBusinessStatus s3 = mkStatus(t.getId(), "商务谈判", 60, 3);
        CrmBusinessStatus s4 = mkStatus(t.getId(), "合同签订", 90, 4);
        statusRepo.saveAll(List.of(s1, s2, s3, s4));

        // 创建商机
        CrmBusiness b = new CrmBusiness();
        b.setName("大商机X");
        b.setCustomerId(cu.getId());
        b.setOwnerUserId(1L);
        b.setStatusTypeId(t.getId());
        b.setStatusId(s1.getId()); // 起点
        b.setTotalProductPrice(100_000L);
        b.setDiscountPercent(BigDecimal.valueOf(90));
        // 手动触发重算(保存时 DataProxy 会做,测试直接调静态方法)
        xyz.herz.ep.crm.core.CrmBusinessStateProxy.recalcTotal(b);
        businessRepo.save(b);
        assertEquals(90_000L, b.getTotalPrice()); // 10w × 90%

        // 推进到 阶段3(s3)
        businessAdvanceHandler.exec(List.of(b), "", new String[]{"FORWARD"}); // s2
        businessAdvanceHandler.exec(List.of(b), "", new String[]{"FORWARD"}); // s3
        assertEquals(s3.getId(),
            businessRepo.findById(b.getId()).orElseThrow().getStatusId());

        // 回退
        businessAdvanceHandler.exec(List.of(b), "", new String[]{"BACK"});
        assertEquals(s2.getId(),
            businessRepo.findById(b.getId()).orElseThrow().getStatusId());

        // 赢单 -> 客户 dealStatus 变 1
        businessEndHandler.exec(List.of(b), "客户已签约", new String[]{"WIN"});
        CrmBusiness re = businessRepo.findById(b.getId()).orElseThrow();
        assertEquals(CrmDictEnums.BusinessEndStatus.WON.code, re.getEndStatus());
        assertEquals("客户已签约", re.getEndRemark());
        assertEquals(CrmDictEnums.DealStatus.DEALT.code,
            customerRepo.findById(cu.getId()).orElseThrow().getDealStatus());
    }

    /** 6. 公海回收扫描 */
    @Test
    void pool_recycle_release_expired_customers() {
        // 配置:2 天未跟进 或 2 天未成交都释放
        CrmCustomerPoolConfig cfg = new CrmCustomerPoolConfig();
        cfg.setEnabled(true);
        cfg.setContactExpireDays(2);
        cfg.setDealExpireDays(2);
        poolRepo.save(cfg);

        // 对照组:今天刚跟进,不会释放
        CrmCustomer fresh = new CrmCustomer();
        fresh.setName("活跃客户");
        fresh.setOwnerUserId(1L);
        fresh.setOwnerTime(java.time.LocalDateTime.now());
        fresh.setContactLastTime(java.time.LocalDateTime.now());
        customerRepo.save(fresh);

        // 目标组:10 天没跟进,肯定释放
        CrmCustomer stale = new CrmCustomer();
        stale.setName("很久没跟进的客户");
        stale.setOwnerUserId(2L);
        stale.setOwnerTime(java.time.LocalDateTime.now().minusDays(10));
        stale.setContactLastTime(java.time.LocalDateTime.now().minusDays(10));
        customerRepo.save(stale);

        recycleJob.run();

        CrmCustomer stillOwned = customerRepo.findById(fresh.getId()).orElseThrow();
        assertNotNull(stillOwned.getOwnerUserId(), "刚跟进的客户不该被释放");

        CrmCustomer released = customerRepo.findById(stale.getId()).orElseThrow();
        assertNull(released.getOwnerUserId(), "超期未跟进,应该被释放回公海(ownerUserId = null)");
        assertNull(released.getOwnerTime());
    }

    private CrmBusinessStatus mkStatus(Long typeId, String name, int pct, int sort) {
        CrmBusinessStatus s = new CrmBusinessStatus();
        s.setTypeId(typeId);
        s.setName(name);
        s.setPercent(BigDecimal.valueOf(pct));
        s.setSort(sort);
        return s;
    }

    /** 7. 团队成员 CRUD(P1 数据权限) */
    @Test
    void team_member_crud() {
        // 造一个客户作为业务对象
        CrmCustomer cu = new CrmCustomer();
        cu.setName("团队测试客户");
        cu.setOwnerUserId(1L);
        customerRepo.save(cu);

        // 添加负责人
        CrmTeamMember owner = new CrmTeamMember();
        owner.setBizType(1); // 1=客户
        owner.setBizId(cu.getId());
        owner.setUserId(1L);
        owner.setRole(CrmDictEnums.TeamRole.OWNER.code);
        owner.setLevel(1);
        teamRepo.save(owner);
        assertNotNull(owner.getId());

        // 添加跟进人
        CrmTeamMember follower = new CrmTeamMember();
        follower.setBizType(1);
        follower.setBizId(cu.getId());
        follower.setUserId(2L);
        follower.setRole(CrmDictEnums.TeamRole.FOLLOWER.code);
        follower.setLevel(2);
        teamRepo.save(follower);

        // 添加只读成员
        CrmTeamMember reader = new CrmTeamMember();
        reader.setBizType(1);
        reader.setBizId(cu.getId());
        reader.setUserId(3L);
        reader.setRole(CrmDictEnums.TeamRole.READONLY.code);
        reader.setLevel(3);
        teamRepo.save(reader);

        // 按 bizType+bizId 查询
        List<CrmTeamMember> members = teamRepo.findByBizTypeAndBizId(1, cu.getId());
        assertEquals(3, members.size(), "应该有 3 个团队成员");

        // 改角色:把只读成员提升为跟进人
        reader.setRole(CrmDictEnums.TeamRole.FOLLOWER.code);
        teamRepo.save(reader);
        CrmTeamMember re = teamRepo.findById(reader.getId()).orElseThrow();
        assertEquals(CrmDictEnums.TeamRole.FOLLOWER.code, re.getRole());

        // 删除一个成员
        teamRepo.delete(follower);
        assertEquals(2, teamRepo.findByBizTypeAndBizId(1, cu.getId()).size());
    }

    /** 8. 合同:创建 → 生效 → 回款计划 → 回款确认 → 计划状态变更 */
    @Test
    void contract_and_receivable() {
        // 准备客户
        CrmCustomer cu = new CrmCustomer();
        cu.setName("合同测试客户");
        cu.setOwnerUserId(1L);
        customerRepo.save(cu);

        // 1. 创建合同(默认 status=0 草稿)
        CrmContract c = new CrmContract();
        c.setNo("HT-2026-001");
        c.setName("测试合同一");
        c.setCustomer(cu);
        c.setAmount(new BigDecimal("100000"));
        c.setSignedDate(LocalDate.now());
        c.setStartDate(LocalDate.now());
        c.setEndDate(LocalDate.now().plusYears(1));
        c.setStatus(CrmDictEnums.ContractStatus.DRAFT.code);
        contractRepo.save(c);
        assertEquals(CrmDictEnums.ContractStatus.DRAFT.code, c.getStatus());

        // 2. 合同生效
        String r = contractEffectHandler.exec(List.of(c), "", new String[]{});
        assertTrue(r.contains("成功 1"), () -> "合同生效失败:" + r);
        CrmContract reC = contractRepo.findById(c.getId()).orElseThrow();
        assertEquals(CrmDictEnums.ContractStatus.EFFECTIVE.code, reC.getStatus());

        // 3. 已生效再生效应失败
        String r2 = contractEffectHandler.exec(List.of(c), "", new String[]{});
        assertTrue(r2.contains("成功 0"), () -> "已生效再生效应失败:" + r2);

        // 4. 创建回款计划(分两期,各 50000)
        CrmReceivablePlan p1 = new CrmReceivablePlan();
        p1.setContract(c);
        p1.setPeriodNo(1);
        p1.setPlanAmount(new BigDecimal("50000"));
        p1.setPlanDate(LocalDate.now().plusMonths(1));
        p1.setReceivedAmount(BigDecimal.ZERO);
        p1.setStatus(CrmDictEnums.ReceivableStatus.PENDING.code);
        planRepo.save(p1);

        CrmReceivablePlan p2 = new CrmReceivablePlan();
        p2.setContract(c);
        p2.setPeriodNo(2);
        p2.setPlanAmount(new BigDecimal("50000"));
        p2.setPlanDate(LocalDate.now().plusMonths(2));
        p2.setReceivedAmount(BigDecimal.ZERO);
        p2.setStatus(CrmDictEnums.ReceivableStatus.PENDING.code);
        planRepo.save(p2);

        // 5. 第一笔回款 30000 → 计划1 部分回款
        CrmReceivableRecord rec1 = new CrmReceivableRecord();
        rec1.setContract(c);
        rec1.setPlan(p1);
        rec1.setAmount(new BigDecimal("30000"));
        rec1.setReceivedDate(LocalDate.now());
        recordRepo.save(rec1);

        String rr1 = receivableConfirmHandler.exec(List.of(rec1), "", new String[]{});
        assertTrue(rr1.contains("成功 1"), () -> "确认回款1失败:" + rr1);
        CrmReceivablePlan reP1 = planRepo.findById(p1.getId()).orElseThrow();
        assertEquals(new BigDecimal("30000"), reP1.getReceivedAmount());
        assertEquals(CrmDictEnums.ReceivableStatus.PARTIAL.code, reP1.getStatus());

        // 6. 第二笔回款 20000 → 计划1 累计 50000 → 已回款
        CrmReceivableRecord rec2 = new CrmReceivableRecord();
        rec2.setContract(c);
        rec2.setPlan(p1);
        rec2.setAmount(new BigDecimal("20000"));
        rec2.setReceivedDate(LocalDate.now());
        recordRepo.save(rec2);

        String rr2 = receivableConfirmHandler.exec(List.of(rec2), "", new String[]{});
        assertTrue(rr2.contains("成功 1"), () -> "确认回款2失败:" + rr2);
        reP1 = planRepo.findById(p1.getId()).orElseThrow();
        assertEquals(new BigDecimal("50000"), reP1.getReceivedAmount());
        assertEquals(CrmDictEnums.ReceivableStatus.RECEIVED.code, reP1.getStatus());

        // 7. 已回款的计划再次确认应拒绝
        CrmReceivableRecord rec3 = new CrmReceivableRecord();
        rec3.setContract(c);
        rec3.setPlan(p1);
        rec3.setAmount(new BigDecimal("10000"));
        rec3.setReceivedDate(LocalDate.now());
        recordRepo.save(rec3);

        String rr3 = receivableConfirmHandler.exec(List.of(rec3), "", new String[]{});
        assertTrue(rr3.contains("成功 0") && rr3.contains("已回款"), () -> "已回款计划应拒绝:" + rr3);
        // 金额不变
        reP1 = planRepo.findById(p1.getId()).orElseThrow();
        assertEquals(new BigDecimal("50000"), reP1.getReceivedAmount());

        // 8. 合同作废
        String rv = contractVoidHandler.exec(List.of(c), "", new String[]{});
        assertTrue(rv.contains("成功 1"), () -> "合同作废失败:" + rv);
        CrmContract reCVoid = contractRepo.findById(c.getId()).orElseThrow();
        assertEquals(CrmDictEnums.ContractStatus.VOID.code, reCVoid.getStatus());

        // 9. 已作废合同再作废应失败
        String rv2 = contractVoidHandler.exec(List.of(c), "", new String[]{});
        assertTrue(rv2.contains("成功 0"), () -> "已作废再作废应失败:" + rv2);
    }
}
