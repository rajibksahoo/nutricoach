package com.nutricoach.progress.repository;

import com.nutricoach.progress.entity.ProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressLogRepository extends JpaRepository<ProgressLog, UUID> {

    List<ProgressLog> findByClientIdAndCoachIdOrderByLoggedDateDesc(UUID clientId, UUID coachId);

    List<ProgressLog> findByClientIdAndCoachIdAndLoggedDateBetweenOrderByLoggedDateAsc(
            UUID clientId, UUID coachId, LocalDate from, LocalDate to);

    Optional<ProgressLog> findByClientIdAndCoachIdAndLoggedDate(UUID clientId, UUID coachId, LocalDate loggedDate);
}
