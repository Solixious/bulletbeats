package in.bulletbeats.domain.menu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "menu_item_availability_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemAvailabilityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Long changedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime changedAt;

    private Boolean overrideBefore;

    private Boolean overrideAfter;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
