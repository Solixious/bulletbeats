package in.bulletbeats.domain.notification;

public record BillNotificationData(
        String toPhone,
        NotificationChannel channel,
        String customerName,
        String cafeName,
        String billNumber,      // template {{3}}: e.g. BB-20260706-0001
        String date,            // template {{4}}: e.g. 06 Jul 2026, 08:10
        String location,        // template {{5}}: table name or order type
        String itemsSummary,    // template {{6}}: items flattened to one line
        String total,           // template {{7}}: e.g. ₹25.00
        String paidVia,         // template {{8}}: payment method display name
        String formattedText    // free-form fallback when no template configured
) {}
