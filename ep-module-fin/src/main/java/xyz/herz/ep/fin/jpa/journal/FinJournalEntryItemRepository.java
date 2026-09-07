package xyz.herz.ep.fin.jpa.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.journal.FinJournalEntryItem;

/**
 * 凭证明细行 Repository。
 * <p>注意:journal_id 列由 {@code FinJournalEntry.items} 的
 * {@code @OneToMany @JoinColumn(name="journal_id")} 单向维护,
 * FinJournalEntryItem 实体本身无 journalId 字段,故不提供 findByJournalId 派生查询。
 * 需要按凭证查明细时,直接读 {@code FinJournalEntry.getItems()}(OneToOne 立即加载)即可。
 */
public interface FinJournalEntryItemRepository extends JpaRepository<FinJournalEntryItem, Long> {
}
