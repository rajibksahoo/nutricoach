package com.nutricoach.messaging.service;

import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.messaging.dto.ConversationSummaryResponse;
import com.nutricoach.messaging.dto.MessageResponse;
import com.nutricoach.messaging.dto.SendMessageRequest;
import com.nutricoach.messaging.entity.Message;
import com.nutricoach.messaging.mapper.MessageMapper;
import com.nutricoach.messaging.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ClientRepository clientRepository;
    private final MessageMapper messageMapper;

    /** Returns all clients for a coach as conversations, sorted by most recent message (clients with no messages appear last). */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(UUID coachId) {
        List<Client> clients = clientRepository.findByCoachIdAndDeletedAtIsNull(coachId);

        return clients.stream()
                .map(client -> buildSummary(coachId, client))
                .sorted(Comparator.comparing(
                        ConversationSummaryResponse::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    /** Returns all messages for a specific coach-client conversation (oldest first). */
    @Transactional
    public List<MessageResponse> getMessages(UUID coachId, UUID clientId) {
        validateClientBelongsToCoach(clientId, coachId);
        // Mark all inbound client messages as read when coach opens the thread
        messageRepository.markAllAsRead(coachId, clientId, Instant.now());
        return messageRepository.findByCoachIdAndClientIdOrderByCreatedAtAsc(coachId, clientId)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    /** Sends a message from the coach to a client. */
    @Transactional
    public MessageResponse sendMessage(UUID coachId, UUID clientId, SendMessageRequest request) {
        validateClientBelongsToCoach(clientId, coachId);

        Message message = Message.builder()
                .coachId(coachId)
                .clientId(clientId)
                .senderType(Message.SenderType.COACH)
                .content(request.content().trim())
                .build();

        return messageMapper.toResponse(messageRepository.save(message));
    }

    /** Client opens their thread: fetch messages + mark coach's messages as read. */
    @Transactional
    public List<MessageResponse> getMessagesForClient(UUID clientId, UUID coachId) {
        messageRepository.markCoachMessagesAsRead(coachId, clientId, Instant.now());
        return messageRepository.findByCoachIdAndClientIdOrderByCreatedAtAsc(coachId, clientId)
                .stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    /** Client sends a message to their coach. */
    @Transactional
    public MessageResponse sendMessageFromClient(UUID clientId, UUID coachId, SendMessageRequest request) {
        Message message = Message.builder()
                .coachId(coachId)
                .clientId(clientId)
                .senderType(Message.SenderType.CLIENT)
                .content(request.content().trim())
                .build();
        return messageMapper.toResponse(messageRepository.save(message));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private ConversationSummaryResponse buildSummary(UUID coachId, Client client) {
        Optional<Message> latest =
                messageRepository.findLatestByCoachIdAndClientId(coachId, client.getId());
        long unread = messageRepository.countUnreadByCoachIdAndClientId(coachId, client.getId());

        return new ConversationSummaryResponse(
                client.getId(),
                client.getName(),
                client.getPhone(),
                latest.map(Message::getContent).orElse(null),
                latest.map(Message::getSenderType).orElse(null),
                latest.map(Message::getCreatedAt).orElse(null),
                unread
        );
    }

    private void validateClientBelongsToCoach(UUID clientId, UUID coachId) {
        clientRepository.findByIdAndCoachIdAndDeletedAtIsNull(clientId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Client not found"));
    }
}
