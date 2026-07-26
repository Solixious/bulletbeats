package in.bulletbeats.domain.notification;

import java.util.LinkedHashMap;
import java.util.Map;

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
) implements TemplateNotification {

    /**
     * Variables match the approved WhatsappTemplate#BILL_RECEIPT placeholders:
     *   {{1}} = customer name
     *   {{2}} = bill number (e.g. BB-20260706-0001)
     *   {{3}} = date (e.g. 06 Jul 2026, 08:10)
     *   {{4}} = items summary — single line (e.g. Tea x1 ₹25.00 | Coffee x2 ₹50.00)
     *   {{5}} = total (e.g. ₹25.00)
     *   {{6}} = payment method (e.g. Cash)
     *
     * Template body:
     *   Hi {{1}}. Here is the summary of your bill from Bullet Beats Cafe.
     *
     *   *Invoice Details*
     *   • Bill Number: #{{2}}
     *   • Date of Issue: {{3}}
     *
     *   *Order Summary*
     *   Your ordered items include: {{4}}
     *
     *   *Payment Details*
     *   • Total Amount Due: {{5}}
     *   • Payment Method: {{6}}
     *
     *   Have a wonderful day!
     */
    @Override
    public Map<String, String> templateVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", orEmpty(customerName));
        vars.put("2", orEmpty(billNumber));
        vars.put("3", orEmpty(date));
        vars.put("4", orEmpty(itemsSummary));
        vars.put("5", orEmpty(total));
        vars.put("6", orEmpty(paidVia));
        return vars;
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }
}
