package com.nutricoach.client.controller;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.notifications.entity.NotificationLog;
import com.nutricoach.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Sending a client their portal link.
 *
 * <p>Until this existed a coach had no way to give a client access at all: the
 * only UI producing a portal link was gated behind dev mode, so in production
 * the client portal was unreachable.
 *
 * <p>The link carries no token or coach id. A client's phone identifies exactly
 * one coach (changeset 028), so signing in needs nothing but their number.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client detail", description = "Training stats, activity feed and coach notes for one client")
public class ClientInviteController {

    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Value("${app.portal-url:}")
    private String portalUrl;

    @PostMapping("/invite")
    @Operation(summary = "Send the client their portal link over WhatsApp",
            description = "Messages the client's WhatsApp number (falling back to their phone) with a link to the portal sign-in")
    public ResponseEntity<ApiResponse<Void>> sendInvite(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();

        Client client = clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));

        if (!StringUtils.hasText(portalUrl)) {
            // Deliberately loud: a WhatsApp message pointing at nothing is worse
            // than a coach being told the server is not configured yet.
            throw NutriCoachException.badRequest(
                    "The portal URL is not configured on the server, so the invite cannot be sent. "
                            + "Set PORTAL_URL and try again, or copy the link and send it yourself.");
        }

        String phone = StringUtils.hasText(client.getWhatsappNumber())
                ? client.getWhatsappNumber()
                : client.getPhone();

        String message = String.format(
                "Hi %s! Your coach has set up your NutriCoach account. "
                        + "Sign in at %s/portal/login using this mobile number to see your plans and log progress. - NutriCoach",
                client.getName(), portalUrl.replaceAll("/+$", ""));

        notificationService.sendWhatsApp(
                coachId, client.getId(), phone, NotificationLog.Type.PORTAL_INVITE, message);

        return ResponseEntity.ok(ApiResponse.ok("Invite sent", null));
    }
}
