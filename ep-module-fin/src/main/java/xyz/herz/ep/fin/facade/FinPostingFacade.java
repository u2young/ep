package xyz.herz.ep.fin.facade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 财务对外统一过账门面(其他模块通过此 facade 触发 GL 凭证生成)。
 * <p>设计原则:
 * <ul>
 *   <li>所有调用方必须在 {@code @Transactional} 上下文中调用,失败整体回滚</li>
 *   <li>{@link #post(PostingRequest)} 内部校验 sum(debit) == sum(credit),不平抛异常阻断</li>
 *   <li>{@link #cancel(String, Long)} 反向冲销(生成红字凭证)</li>
 * </ul>
 *
 * <p>典型用法(项目模块触发项目费用入账):
 * <pre>
 * &#64;Autowired(required = false) FinPostingFacade posting;
 * if (posting != null) {
 *     posting.post(new FinPostingFacade.PostingRequest(
 *         JournalSourceType.PROJECT_EXPENSE.code, claim.getId(), claim.getNo(),
 *         LocalDate.now(),
 *         List.of(new PostingLine("6602.01", claim.getSanctionedAmount(), null,
 *                  "Employee", claim.getEmployeeId(), claim.getEmployeeName(),
 *                  costCenterId, "项目费用")),
 *         "项目费用入账"));
 * }
 * </pre>
 */
public interface FinPostingFacade {

    /** 凭证明细行 */
    record PostingLine(
        String accountCode,           // fin_account.code
        BigDecimal debit,            // 借方金额(可 null,与 credit 互斥)
        BigDecimal credit,           // 贷方金额(可 null,与 debit 互斥)
        String partyType,            // 可选:Customer/Supplier/Employee
        Long partyId,                // 可选:跨模块快照 id
        String partyName,            // 可选:跨模块快照名称(往来方名称)
        Long costCenterId,           // 可选:fin_cost_center.id
        String remark                // 可选:行备注
    ) {}

    /** 过账请求 */
    record PostingRequest(
        String sourceType,           // JournalSourceType.code,如 "SalesInvoice"
        Long sourceId,               // 来源单据 id
        String sourceNo,             // 来源单据号(快照)
        LocalDate postingDate,       // 凭证日期
        List<PostingLine> lines,     // 明细行(必须借贷平衡)
        String remark                // 凭证备注
    ) {}

    /**
     * 生成 GL 凭证。返回 FinJournalEntry.id。失败抛异常(不平/科目不存在)。
     */
    Long post(PostingRequest req);

    /**
     * 反向冲销:按 sourceType + sourceId 找原凭证,生成等额反向凭证(原凭证不变)。
     * 若找不到原凭证,静默返回(幂等,允许重复调用)。
     */
    void cancel(String sourceType, Long sourceId);
}
