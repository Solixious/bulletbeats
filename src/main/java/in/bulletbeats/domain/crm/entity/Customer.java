package in.bulletbeats.domain.crm.entity;

import in.bulletbeats.domain.notification.NotificationChannel;
import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private LocalDateTime lastVisitDate;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private NotificationChannel notificationPreference = NotificationChannel.WHATSAPP;

    /** Daily promo rotation bucket, 1 (Monday) .. 7 (Sunday). */
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int promoBucket;

    @Column(nullable = false)
    @Builder.Default
    private long tenantId = 1L;

    public boolean isEligibleForStudentDiscount() {
        return isStudent && name != null && !name.isBlank();
    }

    /** Digits-only number for wa.me links; bare 10-digit numbers are assumed Indian (+91). */
    public String getWhatsAppNumber() {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }

    public String getMaskedPhone() {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        return "******" + phone.substring(phone.length() - 4);
    }
}
