package xyz.herz.ep.fin.jpa.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.fin.entity.journal.FinJournalEntry;

import java.util.List;
import java.util.Optional;

public interface FinJournalEntryRepository extends JpaRepository<FinJournalEntry, Long> {
    Optional<FinJournalEntry> findByNo(String no);

    /** 反向冲销:按来源单据查找原凭证(用于 cancel) */
    List<FinJournalEntry> findBySourceTypeAndSourceId(String sourceType, Long sourceId);
}
