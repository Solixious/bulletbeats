package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String whatsappText;

    private LocalDate expectedDeliveryDate;

    private BigDecimal totalAmount;

    private LocalDateTime orderedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Long tenantId;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
