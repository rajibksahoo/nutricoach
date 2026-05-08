package com.nutricoach.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.nutricoach.messaging.entity.Message;
import com.nutricoach.messaging.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MessagingIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired JwtService jwtService;

    private Coach coach;
    private Coach otherCoach;
    private Client client;          // belongs to coach
    private Client silentClient;    // belongs to coach, no messages
    private Client otherCoachClient; // belongs to otherCoach — for cross-tenant tests
    private String coachJwt;
    private String otherCoachJwt;
    private String clientJwt;

    @BeforeEach
    void setup() {
        // Wipe any leftover state from prior runs.
        for (String phone : new String[] { "9700000001", "9700000099" }) {
            coachRepository.findByPhone(phone).ifPresent(existing -> {
                clientRepository.findAllByCoachId(existing.getId()).forEach(c -> {
                    messageRepository.deleteAll(
                            messageRepository.findByCoachIdAndClientIdOrderByCreatedAtAsc(existing.getId(), c.getId()));
                });
                clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
                coachRepository.delete(existing);
            });
        }

        coach = coachRepository.save(Coach.builder()
                .phone("9700000001")
                .name("Messaging Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        otherCoach = coachRepository.save(Coach.builder()
                .phone("9700000099")
                .name("Other Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        client = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9700000002")
                .name("Talking Client")
                .status(Client.Status.ACTIVE)
                .build());

        silentClient = clientRepository.save(Client.builder()
                .coachId(coach.getId())
                .phone("9700000003")
                .name("Silent Client")
                .status(Client.Status.ACTIVE)
                .build());

        otherCoachClient = clientRepository.save(Client.builder()
                .coachId(otherCoach.getId())
                .phone("9700000098")
                .name("Other Coach's Client")
                .status(Client.Status.ACTIVE)
                .build());

        coachJwt      = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
        otherCoachJwt = jwtService.generateToken(otherCoach.getPhone(), otherCoach.getId(), "ROLE_COACH");
        clientJwt     = jwtService.generateClientToken(client.getPhone(), client.getId(), coach.getId());
    }

    // ── Conversations list ────────────────────────────────────────────────────

    @Test
    void listConversations_returnsAllClients_includingOnesWithoutMessages() throws Exception {
        seedMessage(coach.getId(), client.getId(), Message.SenderType.CLIENT, "Hi coach!", null);

        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                // The client with the recent message should sort first.
                .andExpect(jsonPath("$.data[0].clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data[0].lastMessage").value("Hi coach!"))
                .andExpect(jsonPath("$.data[0].unreadCount").value(1))
                .andExpect(jsonPath("$.data[0].lastSenderType").value("CLIENT"))
                // The silent client appears last with null preview.
                .andExpect(jsonPath("$.data[1].clientId").value(silentClient.getId().toString()))
                .andExpect(jsonPath("$.data[1].lastMessage").doesNotExist())
                .andExpect(jsonPath("$.data[1].unreadCount").value(0));
    }

    @Test
    void listConversations_doesNotIncludeOtherCoachsClients() throws Exception {
        seedMessage(otherCoach.getId(), otherCoachClient.getId(), Message.SenderType.CLIENT, "leak?", null);

        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2)) // only this coach's two clients
                .andExpect(jsonPath("$.data[?(@.clientName=='Other Coach\\'s Client')]").isEmpty());
    }

    @Test
    void listConversations_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    // ── Get thread ────────────────────────────────────────────────────────────

    @Test
    void getThread_returnsMessagesOldestFirst_andMarksUnreadAsRead() throws Exception {
        seedMessage(coach.getId(), client.getId(), Message.SenderType.CLIENT, "first ping", null);
        seedMessage(coach.getId(), client.getId(), Message.SenderType.COACH,  "got it",     Instant.now());
        seedMessage(coach.getId(), client.getId(), Message.SenderType.CLIENT, "thanks",     null);

        mockMvc.perform(get("/api/v1/messages/clients/{clientId}", client.getId())
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].content").value("first ping"))
                .andExpect(jsonPath("$.data[1].content").value("got it"))
                .andExpect(jsonPath("$.data[2].content").value("thanks"))
                // All three should now be marked read.
                .andExpect(jsonPath("$.data[0].read").value(true))
                .andExpect(jsonPath("$.data[2].read").value(true));

        // Following list call should show 0 unread.
        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(jsonPath("$.data[0].unreadCount").value(0));
    }

    @Test
    void getThread_emptyConversation_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/messages/clients/{clientId}", silentClient.getId())
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void getThread_otherCoachsClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/messages/clients/{clientId}", otherCoachClient.getId())
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(status().isNotFound());
    }

    // ── Send message (coach → client) ─────────────────────────────────────────

    @Test
    void sendMessage_validBody_returns201_andPersistsAsCoach() throws Exception {
        mockMvc.perform(post("/api/v1/messages/clients/{clientId}", client.getId())
                        .header("Authorization", "Bearer " + coachJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Hey, how's training?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderType").value("COACH"))
                .andExpect(jsonPath("$.data.content").value("Hey, how's training?"))
                .andExpect(jsonPath("$.data.read").value(false))
                .andExpect(jsonPath("$.data.clientId").value(client.getId().toString()));

        // Should now appear as the latest message in the conversation list.
        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(jsonPath("$.data[0].lastSenderType").value("COACH"))
                .andExpect(jsonPath("$.data[0].lastMessage").value("Hey, how's training?"));
    }

    @Test
    void sendMessage_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/messages/clients/{clientId}", client.getId())
                        .header("Authorization", "Bearer " + coachJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendMessage_otherCoachsClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/messages/clients/{clientId}", otherCoachClient.getId())
                        .header("Authorization", "Bearer " + coachJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "ping"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendMessage_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/messages/clients/{clientId}", client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "ping"))))
                .andExpect(status().isUnauthorized());
    }

    // ── Portal mirror (client → coach) ────────────────────────────────────────

    @Test
    void portal_getThread_returnsMessages_andMarksCoachMessagesAsRead() throws Exception {
        seedMessage(coach.getId(), client.getId(), Message.SenderType.COACH, "Welcome!", null);
        seedMessage(coach.getId(), client.getId(), Message.SenderType.COACH, "Plan attached.", null);

        mockMvc.perform(get("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].read").value(true))
                .andExpect(jsonPath("$.data[1].read").value(true));
    }

    @Test
    void portal_sendMessage_returns201_andPersistsAsClient() throws Exception {
        mockMvc.perform(post("/api/v1/portal/messages")
                        .header("Authorization", "Bearer " + clientJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Quick question"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderType").value("CLIENT"))
                .andExpect(jsonPath("$.data.content").value("Quick question"));

        // Coach should see it as a new unread message.
        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + coachJwt))
                .andExpect(jsonPath("$.data[0].clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.data[0].unreadCount").value(1))
                .andExpect(jsonPath("$.data[0].lastSenderType").value("CLIENT"));
    }

    // Other coach can read their own thread but not this coach's thread —
    // implicit via tenant isolation but covered explicitly here for completeness.
    @Test
    void otherCoach_cannotReadThisCoachsThread() throws Exception {
        seedMessage(coach.getId(), client.getId(), Message.SenderType.CLIENT, "private note", null);

        mockMvc.perform(get("/api/v1/messages/clients/{clientId}", client.getId())
                        .header("Authorization", "Bearer " + otherCoachJwt))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("UnusedReturnValue")
    private UUID seedMessage(UUID coachId, UUID clientId, Message.SenderType senderType, String content, Instant readAt) {
        Message m = messageRepository.save(Message.builder()
                .coachId(coachId)
                .clientId(clientId)
                .senderType(senderType)
                .content(content)
                .readAt(readAt)
                .build());
        return m.getId();
    }
}
