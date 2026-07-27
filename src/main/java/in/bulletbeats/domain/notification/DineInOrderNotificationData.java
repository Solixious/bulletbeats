package in.bulletbeats.domain.notification;

import java.util.LinkedHashMap;
import java.util.Map;

public record DineInOrderNotificationData(
        String toPhone,
        NotificationChannel channel,
        String billNumber,      // template {{1}}: e.g. BB-20260706-0001
        String customerName,    // template {{2}}
        String customerPhone,   // template {{3}}
        String tableNumber,     // template {{4}}: e.g. Table 5
        String itemsSummary,    // template {{5}}: items flattened to one line
        String total,           // template {{6}}: e.g. ₹250.00
        String formattedText    // free-form fallback when no template configured
) implements TemplateNotification {

    /**
     * Variables match the approved WhatsappTemplate#ORDER_RECEIVED_DINE_IN placeholders:
     *   {{1}} = bill number (e.g. BB-20260706-0001)
     *   {{2}} = customer name
     *   {{3}} = customer phone
     *   {{4}} = table number (e.g. Table 5)
     *   {{5}} = items summary — single line (e.g. 2x Burger, 1x Coke)
     *   {{6}} = total (e.g. ₹250.00)
     *
     * Template body:
     *   New dine-in order via QR at Bullet Beats Cafe.
     *
     *   *Order Details*
     *   • Bill Number: #{{1}}
     *   • Customer: {{2}}
     *   • Phone: {{3}}
     *   • Table: {{4}}
     *
     *   *Items*
     *   {{5}}
     *
     *   *Total*: {{6}}
     */
    @Override
    public Map<String, String> templateVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", orEmpty(billNumber));
        vars.put("2", orEmpty(customerName));
        vars.put("3", orEmpty(customerPhone));
        vars.put("4", orEmpty(tableNumber));
        vars.put("5", orEmpty(itemsSummary));
        vars.put("6", orEmpty(total));
        return vars;
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }
}
