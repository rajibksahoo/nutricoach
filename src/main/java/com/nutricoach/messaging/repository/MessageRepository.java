package com.nutricoach.messaging.repository;

import com.nutricoach.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** All messages for a coach-client conversation, oldest first. */
    List<Message> findByCoachIdAndClientIdOrderByCreatedAtAsc(UUID coachId, UUID clientId);

    /** Distinct client IDs that have had at least one message with this coach. */
    @Query("SELECT DISTINCT m.clientId FROM Message m WHERE m.coachId = :coachId ORDER BY m.clientId")
    List<UUID> findDistinctClientIdsByCoachId(@Param("coachId") UUID coachId);

    /** Most recent message per conversation — used to build conversation summaries. */
    @Query("""
        SELECT m FROM Message m
        WHERE m.coachId = :coachId AND m.clientId = :clientId
        ORDER BY m.createdAt DESC
        LIMIT 1
        """)
    java.util.Optional<Message> findLatestByCoachIdAndClientId(
            @Param("coachId") UUID coachId,
            @Param("clientId") UUID clientId);

    /** Count unread messages (sent by client, not yet read) for a conversation. */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.coachId = :coachId AND m.clientId = :clientId
          AND m.senderType = 'CLIENT' AND m.readAt IS NULL
        """)
    long countUnreadByCoachIdAndClientId(
            @Param("coachId") UUID coachId,
            @Param("clientId") UUID clientId);

    /** Mark all client messages in a conversation as read (called when coach opens the thread). */
    @Modifying
    @Query("""
        UPDATE Message m SET m.readAt = :now
        WHERE m.coachId = :coachId AND m.clientId = :clientId
          AND m.senderType = 'CLIENT' AND m.readAt IS NULL
        """)
    void markAllAsRead(
            @Param("coachId") UUID coachId,
            @Param("clientId") UUID clientId,
            @Param("now") Instant now);

    /** Mark all coach messages in a conversation as read (called when client opens the thread). */
    @Modifying
    @Query("""
        UPDATE Message m SET m.readAt = :now
        WHERE m.coachId = :coachId AND m.clientId = :clientId
          AND m.senderType = 'COACH' AND m.readAt IS NULL
        """)
    void markCoachMessagesAsRead(
            @Param("coachId") UUID coachId,
            @Param("clientId") UUID clientId,
            @Param("now") Instant now);
}
