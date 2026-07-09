package in.bulletbeats.domain.whatsapp.repository;

import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {

    List<WhatsappMessage> findByFromNumberOrderByReceivedAtAsc(String fromNumber);

    boolean existsByTwilioSid(String twilioSid);

    /**
     * Returns one row per sender with their latest message body, timestamp, and total count.
     * Ordered newest-first by last received message.
     */
    @Query(value = """
        SELECT
            m.from_number,
            MAX(m.received_at)                                                          AS last_received_at,
            COUNT(m.id)                                                                 AS message_count,
            (SELECT m2.body FROM whatsapp_messages m2
             WHERE m2.from_number = m.from_number
             ORDER BY m2.received_at DESC LIMIT 1)                                      AS last_body
        FROM whatsapp_messages m
        GROUP BY m.from_number
        ORDER BY MAX(m.received_at) DESC
        """, nativeQuery = true)
    List<ConversationRow> findConversationSummaries();

    interface ConversationRow {
        String getFromNumber();
        LocalDateTime getLastReceivedAt();
        Long getMessageCount();
        String getLastBody();
    }
}
