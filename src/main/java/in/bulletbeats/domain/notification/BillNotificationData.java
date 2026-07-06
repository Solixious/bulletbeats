package in.bulletbeats.domain.notification;

public record BillNotificationData(
        String toPhone,
        NotificationChannel channel,
        String customerName,
        String cafeName,
        String breakdown,      // template {{3}}: bill#, date, items, totals, payment
        String formattedText   // free-form fallback when no template configured
) {}
