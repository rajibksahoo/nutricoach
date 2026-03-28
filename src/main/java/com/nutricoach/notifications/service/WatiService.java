package com.nutricoach.notifications.service;

import com.nutricoach.common.config.WatiProperties;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@EnableConfigurationProperties(WatiProperties.class)
public class WatiService {

    private final WatiProperties watiProperties;
    private final RestClient restClient;

    public WatiService(WatiProperties watiProperties) {
        this.watiProperties = watiProperties;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Sends a WhatsApp text message via WATI.
     * In dev mode (token starts with "local-"), logs the message and returns a mock ID.
     *
     * @param phone   recipient phone number (with country code, e.g. "919876543210")
     * @param message message text to send
     * @return external message ID from WATI, or "local-mock-id" in dev mode
     */
    public String sendTextMessage(String phone, String message) {
        if (watiProperties.getApiToken().startsWith("local-")) {
            log.info("[DEV] WATI message to {}: {}", phone, message);
            return "local-mock-id";
        }

        String url = watiProperties.getApiEndpoint()
                + "/api/v1/sendSessionMessage/" + phone
                + "?access_token=" + watiProperties.getApiToken();

        try {
            Map<String, String> body = Map.of("messageText", message);

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String externalId = response != null && response.containsKey("id")
                    ? String.valueOf(response.get("id"))
                    : "unknown";

            log.info("WATI message sent to {}, externalId={}", phone, externalId);
            return externalId;

        } catch (Exception e) {
            log.error("Failed to send WATI message to {}: {}", phone, e.getMessage());
            throw NutriCoachException.badRequest("Failed to send WhatsApp message");
        }
    }
}
