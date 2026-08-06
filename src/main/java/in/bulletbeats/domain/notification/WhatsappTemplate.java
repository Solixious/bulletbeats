package in.bulletbeats.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catalog of Twilio WhatsApp Content Templates used across the app.
 * Each entry's {@code configKey} maps to a content SID configured under
 * {@code twilio.templates.<configKey>} (see TwilioProperties).
 * <p>
 * BILL_RECEIPT, ORDER_RECEIVED_DINE_IN and ORDER_RECEIVED_DELIVERY are wired
 * to send paths — the rest are declared here so future notifications have a
 * fixed place to register without re-plumbing the notification service.
 */
@Getter
@RequiredArgsConstructor
public enum WhatsappTemplate {

    BILL_RECEIPT("bill-receipt", TemplateAudience.CUSTOMER, "Bill Receipt"),
    FEEDBACK_REQUEST("feedback-request", TemplateAudience.CUSTOMER, "Feedback Request"),
    MARKETING_PROMOTION("marketing-promotion", TemplateAudience.CUSTOMER, "Marketing / Promotion"),
    ORDER_RECEIVED_DINE_IN("order-received-dine-in", TemplateAudience.STAFF, "Order Received — Dine-In (QR)"),
    ORDER_RECEIVED_DELIVERY("order-received-delivery", TemplateAudience.STAFF, "Order Received — Direct Delivery"),
    BILL_PAID_ADMIN_COPY("bill-paid-admin-copy", TemplateAudience.STAFF, "Bill Paid — Admin Copy");

    /** Key used to look up the content SID under {@code twilio.templates.<configKey>}. */
    private final String configKey;
    private final TemplateAudience audience;
    private final String label;
}
