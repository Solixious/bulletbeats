package in.bulletbeats.domain.whatsapp.repository;

import in.bulletbeats.domain.whatsapp.entity.MessageDirection;
import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {

    List<WhatsappMessage> findByFromNumberOrderByReceivedAtAsc(String fromNumber);

    boolean existsByTwilioSid(String twilioSid);

    Optional<WhatsappMessage> findTopByFromNumberAndDirectionOrderByReceivedAtDesc(
            String fromNumber, MessageDirection direction);

    @Query("SELECT COUNT(m) FROM WhatsappMessage m WHERE m.direction = :direction AND m.isRead = false")
    long countUnread(@Param("direction") MessageDirection direction);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE WhatsappMessage m SET m.isRead = true " +
           "WHERE m.fromNumber = :fromNumber AND m.direction = :direction AND m.isRead = false")
    int markAllAsRead(@Param("fromNumber") String fromNumber,
                      @Param("direction") MessageDirection direction);

    /**
     * One row per sender: last message body, timestamp, total count, and unread INBOUND count.
     * Ordered newest-first.
     */
    @Query(value = """
        SELECT
            m.from_number,
            MAX(m.received_at)                                                               AS last_received_at,
            COUNT(m.id)                                                                      AS message_count,
            SUM(CASE WHEN m.direction = 'INBOUND' AND m.is_read = false THEN 1 ELSE 0 END)  AS unread_count,
            (SELECT m2.body FROM whatsapp_messages m2
             WHERE m2.from_number = m.from_number
             ORDER BY m2.received_at DESC LIMIT 1)                                           AS last_body
        FROM whatsapp_messages m
        GROUP BY m.from_number
        ORDER BY MAX(m.received_at) DESC
        """, nativeQuery = true)
    List<ConversationRow> findConversationSummaries();

    interface ConversationRow {
        String getFromNumber();
        LocalDateTime getLastReceivedAt();
        Long getMessageCount();
        Long getUnreadCount();
        String getLastBody();
    }
}
