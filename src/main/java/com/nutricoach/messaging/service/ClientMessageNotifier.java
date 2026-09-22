package com.nutricoach.messaging.service;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.notifications.entity.NotificationLog;
import com.nutricoach.notifications.repository.NotificationLogRepository;
import com.nutricoach.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Pings a client on WhatsApp when their coach sends them an in-app message.
 *
 * <p>Without this the messaging feature is write-only in practice: the coach
 * types, the client never learns anything arrived, and the coach concludes the
 * product does not work. WhatsApp is the channel Indian clients actually watch.
 *
 * <p>A separate bean on purpose — {@code @Async} is proxy-based, so calling an
 * async method on {@code this} from {@link MessageService} would silently run
 * inline and put a third-party HTTP call on the coach's request thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMessageNotifier {

    /** A thread the client is ignoring still earns one fresh nudge a day. */
    private static final long QUIET_PERIOD_SECONDS = 24 * 60 * 60;

    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${app.portal-url:}")
    private String portalUrl;

    /**
     * Notifies the client, unless doing so would be noise.
     *
     * <p>Runs off the request thread and never throws: a WATI outage must not
     * fail the coach's send, and the message is already persisted by the time
     * this runs. A failure is logged and the {@code NotificationLog} row carries
     * the FAILED status.
     *
     * @param hadUnreadBefore whether the client was already sitting on unread
     *                        coach messages when this one was sent. Computed by
     *                        the caller <em>before</em> the save, because after it
     *                        the new message is itself unread.
     */
    @Async
    public void notifyNewMessage(UUID coachId, UUID clientId, boolean hadUnreadBefore) {
        try {
            if (!shouldNotify(coachId, clientId, hadUnreadBefore)) {
                return;
            }

            Client client = clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                    .orElse(null);
            if (client == null) {
                return;
            }

            String phone = StringUtils.hasText(client.getWhatsappNumber())
                    ? client.getWhatsappNumber()
                    : client.getPhone();
            if (!StringUtils.hasText(phone)) {
                return;
            }

            notificationService.sendWhatsApp(
                    coachId, clientId, phone,
                    NotificationLog.Type.NEW_MESSAGE,
                    buildMessage(client.getName()));

        } catch (Exception e) {
            // Deliberately swallowed. The coach's message is sent either way, and
            // there is no user-facing surface on this thread to report into.
            log.warn("ClientMessageNotifier: WhatsApp ping failed for client={}: {}",
                    clientId, e.getMessage());
        }
    }

    /**
     * One ping per thread the client has caught up on, rather than one per
     * message — a coach typing four lines in a row should not buzz a phone four
     * times, and WATI bills per message.
     */
    private boolean shouldNotify(UUID coachId, UUID clientId, boolean hadUnreadBefore) {
        if (!hadUnreadBefore) {
            return true;
        }
        // Already sitting on unread messages: stay quiet, unless the last ping is
        // old enough that this thread has gone cold.
        return !notificationLogRepository.existsByCoachIdAndClientIdAndTypeAndCreatedAtAfter(
                coachId, clientId, NotificationLog.Type.NEW_MESSAGE,
                Instant.now().minusSeconds(QUIET_PERIOD_SECONDS));
    }

    /**
     * No message content. The body is free-form text on a channel the coach does
     * not control, and a plan or health detail landing in a WhatsApp preview on a
     * shared phone is a privacy problem we do not need to own.
     */
    private String buildMessage(String clientName) {
        String base = String.format(
                "Hi %s! You have a new message from your coach on NutriCoach.", clientName);
        return StringUtils.hasText(portalUrl)
                ? base + " Open " + portalUrl.replaceAll("/+$", "") + " to read and reply."
                : base + " Open the app to read and reply.";
    }
}
