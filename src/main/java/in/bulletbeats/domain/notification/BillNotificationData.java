package in.bulletbeats.domain.notification;

import java.util.LinkedHashMap;
import java.util.Map;

public record BillNotificationData(
        String toPhone,
        NotificationChannel channel,
        String customerName,    // template {{1}}
        String billNumber,      // template {{2}}: e.g. BB-20260706-0001
        String date,            // template {{3}}: e.g. 06 Jul 2026, 08:10
        String location,        // template {{4}}: table name or order type
        String itemsSummary,    // template {{5}}: items flattened to one line
        String total,           // template {{6}}: e.g. ₹25.00
        String paidVia,         // template {{7}}: payment method display name
        String formattedText    // free-form fallback when no template configured
) implements TemplateNotification {

    @Override
    public Map<String, String> templateVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", orEmpty(customerName));
        vars.put("2", orEmpty(billNumber));
        vars.put("3", orEmpty(date));
        vars.put("4", orEmpty(location));
        vars.put("5", orEmpty(itemsSummary));
        vars.put("6", orEmpty(total));
        vars.put("7", orEmpty(paidVia));
        return vars;
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }
}
