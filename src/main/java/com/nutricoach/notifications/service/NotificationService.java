package com.nutricoach.notifications.service;

import com.nutricoach.notifications.entity.NotificationLog;
import com.nutricoach.notifications.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WatiService watiService;
    private final NotificationLogRepository notificationLogRepository;

    /**
     * Sends a WhatsApp message and persists a NotificationLog entry tracking the outcome.
     *
     * @param coachId  owning coach's UUID
     * @param clientId target client's UUID
     * @param phone    recipient phone number (with country code)
     * @param type     notification type (e.g. MEAL_PLAN_SHARE)
     * @param message  message body to send
     */
    @Transactional
    public void sendWhatsApp(UUID coachId, UUID clientId, String phone,
                             NotificationLog.Type type, String message) {
        NotificationLog entry = NotificationLog.builder()
                .coachId(coachId)
                .clientId(clientId)
                .channel(NotificationLog.Channel.WHATSAPP)
                .type(type)
                .status(NotificationLog.Status.PENDING)
                .messageBody(message)
                .build();

        notificationLogRepository.save(entry);

        try {
            String externalId = watiService.sendTextMessage(phone, message);
            entry.setStatus(NotificationLog.Status.SENT);
            entry.setExternalId(externalId);
            entry.setSentAt(Instant.now());
            notificationLogRepository.save(entry);
        } catch (Exception e) {
            entry.setStatus(NotificationLog.Status.FAILED);
            notificationLogRepository.save(entry);
            throw e;
        }
    }
}
