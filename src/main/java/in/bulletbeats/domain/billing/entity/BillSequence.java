package in.bulletbeats.domain.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bill_sequence")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillSequence {

    @Id
    private LocalDate billDate;

    @Column(nullable = false)
    private int lastSeq;
}
