package in.bulletbeats.domain.whatsapp.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappMessage extends BaseEntity {

    /** The customer's phone number (E.164). Thread key for both directions. */
    @Column(name = "from_number", nullable = false, length = 30)
    private String fromNumber;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "twilio_sid", length = 50, unique = true)
    private String twilioSid;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MessageDirection direction = MessageDirection.INBOUND;
}
