package in.bulletbeats.domain.notification;

public record BillNotificationData(
        String toPhone,
        NotificationChannel channel,
        String customerName,    // template {{1}}
        String billNumber,      // template {{2}}: e.g. BB-20260706-0001
        String date,            // template {{3}}: e.g. 06 Jul 2026, 08:10
        String itemsSummary,    // template {{4}}: items flattened to one line
        String total,           // template {{5}}: e.g. ₹25.00
        String paidVia,         // template {{6}}: payment method display name
        String formattedText    // free-form fallback when no template configured
) {}
