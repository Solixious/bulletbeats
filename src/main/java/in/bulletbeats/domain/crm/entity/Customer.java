package in.bulletbeats.domain.crm.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20, unique = true)
    private String phone;

    @Column(length = 150)
    private String email;

    private LocalDate dob;

    @Column(nullable = false)
    @Builder.Default
    private int visitCount = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSpend = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int loyaltyPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVip = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isStudent = false;

    @Column(nullable = false)
    @Builder.Default
    private int studentDiscountCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int notesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long tenantId = 1L;

    public boolean isEligibleForStudentDiscount() {
        return isStudent && name != null && !name.isBlank();
    }

    public String getMaskedPhone() {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "******" + phone.substring(phone.length() - 4);
    }
}
