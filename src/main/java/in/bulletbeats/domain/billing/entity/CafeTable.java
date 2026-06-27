package in.bulletbeats.domain.billing.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.shared.enums.TableStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cafe_tables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CafeTable extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TableStatus status = TableStatus.FREE;

    @Column(length = 100)
    private String qrCode;

    @Column(length = 255)
    private String qrImagePath;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    private java.time.LocalDateTime lastScannedAt;

    @Column(nullable = false)
    @Builder.Default
    private int idleTimeoutMinutes = 10;

    @Column(nullable = false)
    @Builder.Default
    private long tenantId = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    @Column(nullable = false)
    @Builder.Default
    private int xPos = 100;

    @Column(nullable = false)
    @Builder.Default
    private int yPos = 100;

    @Column(nullable = false)
    @Builder.Default
    private int seatCount = 4;

    @Column(nullable = false)
    @Builder.Default
    private int tableWidth = 100;

    @Column(nullable = false)
    @Builder.Default
    private int tableHeight = 68;

    @Column(nullable = false)
    @Builder.Default
    private int rotation = 0;

    public boolean isFree() {
        return status == TableStatus.FREE;
    }

    public boolean isOccupied() {
        return status == TableStatus.OCCUPIED;
    }
}
