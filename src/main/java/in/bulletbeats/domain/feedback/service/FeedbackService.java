package in.bulletbeats.domain.feedback.service;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.service.BillingService;
import in.bulletbeats.domain.feedback.entity.FeedbackRequest;
import in.bulletbeats.domain.feedback.entity.FeedbackStatus;
import in.bulletbeats.domain.feedback.repository.FeedbackRequestRepository;
import in.bulletbeats.domain.notification.FeedbackRequestNotificationData;
import in.bulletbeats.domain.notification.NotificationChannel;
import in.bulletbeats.domain.notification.NotificationService;
import in.bulletbeats.domain.notification.WhatsappTemplate;
import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import in.bulletbeats.domain.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRequestRepository feedbackRequestRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final AppConfigService appConfigService;

    /**
     * Staff-triggered: sends the feedback-request WhatsApp message and opens a
     * PENDING window during which the customer's first reply is captured.
     */
    @Transactional
    public void requestFeedback(Long billId) {
        Bill bill = billingService.getBillById(billId);
        if (bill.getCustomer() == null || bill.getCustomer().getPhone() == null) {
            throw new IllegalStateException("Bill has no customer or phone number linked");
        }

        String phone = WhatsappMessageService.toLocalDigits(bill.getCustomer().getPhone());
        int windowMinutes = appConfigService.getInt("feedback.window_minutes", 60);
        LocalDateTime now = LocalDateTime.now();

        FeedbackRequest request = FeedbackRequest.builder()
                .bill(bill)
                .customer(bill.getCustomer())
                .phone(phone)
                .requestedAt(now)
                .expiresAt(now.plusMinutes(windowMinutes))
                .build();
        feedbackRequestRepository.save(request);

        notificationService.sendNow(WhatsappTemplate.FEEDBACK_REQUEST, buildNotificationData(bill));
    }

    /**
     * Called from the inbound WhatsApp webhook path for every stored message.
     * Matches the most recent still-open feedback request for this phone and,
     * if found, captures this reply and closes the request. No-op otherwise.
     */
    @Transactional
    public void tryCaptureFeedback(String fromNumber, String body, WhatsappMessage message) {
        if (body == null || body.isBlank()) {
            return;
        }
        String phone = WhatsappMessageService.toLocalDigits(fromNumber);
        feedbackRequestRepository
                .findFirstByPhoneAndStatusAndExpiresAtAfterOrderByRequestedAtDesc(
                        phone, FeedbackStatus.PENDING, LocalDateTime.now())
                .ifPresent(request -> {
                    request.setStatus(FeedbackStatus.RECEIVED);
                    request.setResponseBody(body);
                    request.setRespondedAt(LocalDateTime.now());
                    request.setResponseMessage(message);
                    log.info("Feedback captured for bill={} phone={}",
                            request.getBill().getId(), phone);
                });
    }

    @Transactional(readOnly = true)
    public List<FeedbackRequest> listAll() {
        return feedbackRequestRepository.findAllByOrderByRequestedAtDesc();
    }

    @Transactional
    public void deleteFeedback(Long id) {
        feedbackRequestRepository.deleteById(id);
    }

    private FeedbackRequestNotificationData buildNotificationData(Bill bill) {
        NotificationChannel channel = bill.getCustomer().getNotificationPreference() != null
                ? bill.getCustomer().getNotificationPreference()
                : NotificationChannel.WHATSAPP;

        String customerName = bill.getCustomer().getName();
        String cafeName = appConfigService.get("cafe.name", "Bullet Beats Café");

        String formattedText = "Hi " + customerName + ", thanks for visiting " + cafeName
                + "! We'd love your feedback — just reply to this message and let us know how it went.";

        return new FeedbackRequestNotificationData(
                bill.getCustomer().getPhone(), channel, customerName, cafeName, formattedText);
    }
}
