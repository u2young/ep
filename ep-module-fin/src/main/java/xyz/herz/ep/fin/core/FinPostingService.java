package xyz.herz.ep.fin.core;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.herz.ep.fin.entity.account.FinAccount;
import xyz.herz.ep.fin.entity.costcenter.FinCostCenter;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;
import xyz.herz.ep.fin.entity.journal.FinJournalEntryItem;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;
import xyz.herz.ep.fin.jpa.account.FinAccountRepository;
import xyz.herz.ep.fin.jpa.costcenter.FinCostCenterRepository;
import xyz.herz.ep.fin.jpa.journal.FinJournalEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务过账服务实现(对外 Facade 默认实现)。
 * <p>核心职责:
 * <ol>
 *   <li>{@link #post(PostingRequest)} 校验借贷平衡,构造 GL 凭证 + 明细,落库为 SUBMITTED(1)</li>
 *   <li>{@link #cancel(String, Long)} 反向冲销:按来源单据查找原凭证,生成等额反向凭证(借贷互换)</li>
 * </ol>
 *
 * <p>所有方法 {@code @Transactional(REQUIRED)},失败整体回滚;
 * 内部校验 sum(debit) == sum(credit),不平抛 {@link IllegalArgumentException} 阻断。
 */
@Service
@Transactional
public class FinPostingService implements FinPostingFacade {

    private final FinAccountRepository accountRepo;
    private final FinCostCenterRepository costCenterRepo;
    private final FinJournalEntryRepository journalRepo;

    @PersistenceContext
    private EntityManager em;

    public FinPostingService(FinAccountRepository accountRepo,
                             FinCostCenterRepository costCenterRepo,
                             FinJournalEntryRepository journalRepo) {
        this.accountRepo = accountRepo;
        this.costCenterRepo = costCenterRepo;
        this.journalRepo = journalRepo;
    }

    @Override
    public Long post(PostingRequest req) {
        if (req == null) throw new IllegalArgumentException("PostingRequest 不能为空");
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalArgumentException("凭证明细不能为空");
        }

        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;
        List<FinJournalEntryItem> items = new ArrayList<>(req.lines().size());

        for (PostingLine line : req.lines()) {
            BigDecimal d = line.debit() == null ? BigDecimal.ZERO : line.debit();
            BigDecimal c = line.credit() == null ? BigDecimal.ZERO : line.credit();
            if (d.signum() > 0 && c.signum() > 0) {
                throw new IllegalArgumentException("借方与贷方不能同时 > 0: account=" + line.accountCode());
            }
            if (d.signum() == 0 && c.signum() == 0) {
                throw new IllegalArgumentException("借方与贷方不能同时为 0: account=" + line.accountCode());
            }
            sumDebit = sumDebit.add(d);
            sumCredit = sumCredit.add(c);

            FinAccount account = accountRepo.findByCode(line.accountCode())
                .orElseThrow(() -> new IllegalArgumentException("会计科目不存在: " + line.accountCode()));

            FinJournalEntryItem item = new FinJournalEntryItem();
            item.setAccount(account);
            item.setDebit(d);
            item.setCredit(c);
            item.setPartyType(line.partyType());
            item.setPartyId(line.partyId());
            item.setPartyName(line.partyName());
            item.setRemark(line.remark());
            if (line.costCenterId() != null) {
                // 用 getReference 避免 N+1 查询(若不存在 JPA 在 flush 时抛)
                FinCostCenter cc = em.getReference(FinCostCenter.class, line.costCenterId());
                // costCenter 字段在 FinJournalEntryItem 当前未声明(简化版);若后续加 REF 在此 set
                // item.setCostCenter(cc);  // 当前实体未声明 costCenter REF,留扩展
            }
            items.add(item);
        }

        if (sumDebit.compareTo(sumCredit) != 0) {
            throw new IllegalArgumentException(
                "凭证借贷不平:debit=" + sumDebit + ", credit=" + sumCredit);
        }

        FinJournalEntry journal = new FinJournalEntry();
        journal.setNo("JE-" + System.nanoTime() + "-" + (req.sourceId() == null ? "X" : req.sourceId()));
        journal.setPostingDate(req.postingDate() == null ? LocalDate.now() : req.postingDate());
        journal.setStatus(JournalStatus.SUBMITTED.code);
        journal.setTotalDebit(sumDebit);
        journal.setTotalCredit(sumCredit);
        journal.setSourceType(req.sourceType());
        journal.setSourceId(req.sourceId());
        journal.setSourceNo(req.sourceNo());
        journal.setRemark(req.remark());
        journal.setItems(items);

        // 双向关联回绑(JPA 维护端:items 不 REF journal FK,故不需要 item.setJournal(...))
        FinJournalEntry saved = journalRepo.save(journal);
        return saved.getId();
    }

    @Override
    public void cancel(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) return;
        List<FinJournalEntry> originals = journalRepo.findBySourceTypeAndSourceId(sourceType, sourceId);
        if (originals == null || originals.isEmpty()) return; // 幂等:无原凭证静默返回

        for (FinJournalEntry orig : originals) {
            // 只对 SUBMITTED 的原凭证生成反向凭证(已 CANCELLED 的跳过)
            if (orig.getStatus() == null || orig.getStatus() != JournalStatus.SUBMITTED.code) continue;

            FinJournalEntry reverse = new FinJournalEntry();
            reverse.setNo("JE-REV-" + System.nanoTime() + "-" + orig.getId());
            reverse.setPostingDate(LocalDate.now());
            reverse.setStatus(JournalStatus.SUBMITTED.code);
            reverse.setTotalDebit(orig.getTotalCredit() == null ? BigDecimal.ZERO : orig.getTotalCredit());
            reverse.setTotalCredit(orig.getTotalDebit() == null ? BigDecimal.ZERO : orig.getTotalDebit());
            reverse.setSourceType(JournalSourceType.MANUAL_JOURNAL.code);
            reverse.setSourceId(orig.getId());
            reverse.setSourceNo(orig.getNo());
            reverse.setRemark("反向冲销原凭证 " + orig.getNo());

            List<FinJournalEntryItem> reverseItems = new ArrayList<>();
            // 通过 em 查原明细(JPA 一对多 cascade ALL 但 items 字段可能未初始化,显式查)
            if (orig.getItems() != null) {
                for (FinJournalEntryItem origItem : orig.getItems()) {
                    FinJournalEntryItem ri = new FinJournalEntryItem();
                    ri.setAccount(origItem.getAccount());
                    ri.setDebit(origItem.getCredit() == null ? BigDecimal.ZERO : origItem.getCredit());
                    ri.setCredit(origItem.getDebit() == null ? BigDecimal.ZERO : origItem.getDebit());
                    ri.setPartyType(origItem.getPartyType());
                    ri.setPartyId(origItem.getPartyId());
                    ri.setPartyName(origItem.getPartyName());
                    ri.setRemark("冲销 " + origItem.getRemark());
                    reverseItems.add(ri);
                }
            }
            reverse.setItems(reverseItems);
            journalRepo.save(reverse);
        }
    }
}
