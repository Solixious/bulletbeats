package in.bulletbeats.domain.billing.entity;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bill extends BaseEntity {

    @Column(nullable = false, length = 30, unique = true)
    private String billNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafe_table_id", nullable = false)
    private CafeTable cafeTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DiscountType discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean gstInclusive = false;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal studentDiscountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean studentDiscountApplied = false;

    private Long studentDiscountAppliedBy;

    private LocalDateTime studentDiscountAppliedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime confirmedAt;

    private Long transferredFromTableId;

    private LocalDateTime transferredAt;

    @Column(nullable = false)
    @Builder.Default
    private long tenantId = 1L;

    @Builder.Default
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillActivityLog> activityLog = new ArrayList<>();

    public boolean hasStudentDiscount() {
        return studentDiscountApplied
                && studentDiscountAmount != null
                && studentDiscountAmount.signum() > 0;
    }

    public boolean canAddItems() {
        return status.canAddItems();
    }

    public boolean isEditable() {
        return status == BillStatus.DRAFT;
    }
}
