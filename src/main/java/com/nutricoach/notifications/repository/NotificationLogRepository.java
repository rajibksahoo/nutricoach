package com.nutricoach.notifications.repository;

import com.nutricoach.notifications.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByCoachIdAndClientIdOrderByCreatedAtDesc(UUID coachId, UUID clientId);

    List<NotificationLog> findByCoachIdAndTypeAndCreatedAtAfter(
            UUID coachId, NotificationLog.Type type, Instant after);

    boolean existsByCoachIdAndClientIdAndTypeAndCreatedAtAfter(
            UUID coachId, UUID clientId, NotificationLog.Type type, Instant after);
}
