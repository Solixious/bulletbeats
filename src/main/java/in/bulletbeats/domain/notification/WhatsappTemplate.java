package in.bulletbeats.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catalog of Twilio WhatsApp Content Templates used across the app.
 * Each entry's {@code configKey} maps to a content SID configured under
 * {@code twilio.templates.<configKey>} (see TwilioProperties).
 * <p>
 * Only BILL_RECEIPT is currently wired to a send path — the rest are
 * declared here so future notifications have a fixed place to register
 * without re-plumbing the notification service.
 */
@Getter
@RequiredArgsConstructor
public enum WhatsappTemplate {

    BILL_RECEIPT("bill-receipt", TemplateAudience.CUSTOMER, "Bill Receipt"),
    MARKETING_PROMOTION("marketing-promotion", TemplateAudience.CUSTOMER, "Marketing / Promotion"),
    ORDER_RECEIVED("order-received", TemplateAudience.STAFF, "Order Received"),
    BILL_PAID_ADMIN_COPY("bill-paid-admin-copy", TemplateAudience.STAFF, "Bill Paid — Admin Copy");

    /** Key used to look up the content SID under {@code twilio.templates.<configKey>}. */
    private final String configKey;
    private final TemplateAudience audience;
    private final String label;
}
