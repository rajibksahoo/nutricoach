package com.nutricoach.notifications.scheduler;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.notifications.entity.NotificationLog;
import com.nutricoach.notifications.repository.NotificationLogRepository;
import com.nutricoach.notifications.service.NotificationService;
import com.nutricoach.progress.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckInReminderScheduler {

    private final CheckInRepository checkInRepository;
    private final ClientRepository clientRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    /**
     * Runs daily at 08:00 AM IST (02:30 UTC).
     * Sends a WhatsApp reminder to any ACTIVE client who has not logged a check-in
     * in the past 7 days, provided we have not already sent them a reminder in the
     * last 24 hours.
     */
    @Scheduled(cron = "0 30 2 * * *", zone = "UTC")
    public void sendCheckInReminders() {
        log.info("CheckInReminderScheduler: starting daily check-in reminder run");

        List<Client> activeClients =
                clientRepository.findAllByStatusAndDeletedAtIsNull(Client.Status.ACTIVE);

        int remindersSent = 0;

        for (Client client : activeClients) {
            try {
                LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
                boolean hasRecentCheckIn =
                        checkInRepository.existsByClientIdAndCheckInDateAfter(client.getId(), sevenDaysAgo);

                if (hasRecentCheckIn) {
                    continue;
                }

                Instant twentyFourHoursAgo = Instant.now().minusSeconds(24 * 60 * 60);
                boolean alreadyReminded = notificationLogRepository
                        .existsByCoachIdAndClientIdAndTypeAndCreatedAtAfter(
                                client.getCoachId(),
                                client.getId(),
                                NotificationLog.Type.CHECK_IN_REMINDER,
                                twentyFourHoursAgo);

                if (alreadyReminded) {
                    continue;
                }

                String phone = (client.getWhatsappNumber() != null && !client.getWhatsappNumber().isBlank())
                        ? client.getWhatsappNumber()
                        : client.getPhone();

                String message = String.format(
                        "Hi %s! Don't forget to log your check-in today. Your coach is tracking your progress. - NutriCoach",
                        client.getName());

                notificationService.sendWhatsApp(
                        client.getCoachId(),
                        client.getId(),
                        phone,
                        NotificationLog.Type.CHECK_IN_REMINDER,
                        message);

                remindersSent++;

            } catch (Exception e) {
                log.warn("CheckInReminderScheduler: failed to send reminder to client={}: {}",
                        client.getId(), e.getMessage());
            }
        }

        log.info("CheckInReminderScheduler: completed — {} reminder(s) sent out of {} active client(s)",
                remindersSent, activeClients.size());
    }
}
