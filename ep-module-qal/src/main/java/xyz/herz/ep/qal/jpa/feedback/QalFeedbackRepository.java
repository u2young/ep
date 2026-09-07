package xyz.herz.ep.qal.jpa.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.herz.ep.qal.entity.feedback.QalFeedback;

import java.util.List;

public interface QalFeedbackRepository extends JpaRepository<QalFeedback, Long> {

    List<QalFeedback> findByCustomerName(String customerName);

    List<QalFeedback> findByIsHandled(Boolean isHandled);
}
