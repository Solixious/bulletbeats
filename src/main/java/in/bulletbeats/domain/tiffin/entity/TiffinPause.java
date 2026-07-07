package in.bulletbeats.domain.tiffin.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tiffin_pauses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TiffinPause extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private TiffinSubscription subscription;

    @Column(nullable = false)
    private LocalDate pauseFrom;

    @Column
    private LocalDate pauseUntil;

    @Column(length = 255)
    private String note;
}
